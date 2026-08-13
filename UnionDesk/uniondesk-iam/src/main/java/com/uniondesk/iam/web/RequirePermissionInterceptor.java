package com.uniondesk.iam.web;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.iam.core.IamService;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RequirePermissionInterceptor implements HandlerInterceptor {

    private final IamService iamService;

    public RequirePermissionInterceptor(IamService iamService) {
        this.iamService = iamService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission annotation = findRequirePermission(handlerMethod);
        if (annotation == null) {
            return true;
        }
        UserContext context = UserContextHolder.current()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED.message()));
        if (!StringUtils.hasText(annotation.domainIdParam())) {
            if (iamService.hasAnyPermission(context, Arrays.asList(annotation.value()))) {
                return true;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
        long targetDomainId = resolveTargetDomainId(request, annotation.domainIdParam());
        boolean allowed = Arrays.stream(annotation.value())
                .anyMatch(code -> iamService.hasPermissionForDomains(context, code, List.of(targetDomainId)));
        if (allowed) {
            return true;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
    }

    @SuppressWarnings("unchecked")
    private long resolveTargetDomainId(HttpServletRequest request, String domainIdParam) {
        Map<String, String> uriVariables = (Map<String, String>) request
                .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String rawDomainId = uriVariables == null ? null : uriVariables.get(domainIdParam);
        if (rawDomainId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
        try {
            return Long.parseLong(rawDomainId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
    }

    private RequirePermission findRequirePermission(HandlerMethod handlerMethod) {
        RequirePermission methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(),
                RequirePermission.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
    }
}

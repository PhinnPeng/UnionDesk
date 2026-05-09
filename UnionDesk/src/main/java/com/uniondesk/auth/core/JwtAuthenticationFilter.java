package com.uniondesk.auth.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.common.web.ApiResponse;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.iam.core.IamService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/health",
            "/api/v1/readiness",
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/password/reset-request",
            "/api/v1/auth/password/reset",
            "/api/v1/auth/login-config",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/error");

    private final JwtTokenService jwtTokenService;
    private final LoginSessionService loginSessionService;
    private final IamService iamService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            LoginSessionService loginSessionService,
            IamService iamService,
            ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.loginSessionService = loginSessionService;
        this.iamService = iamService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            UserContextHolder.clear();
            SecurityContextHolder.clearContext();
            if (isPublicPath(request.getRequestURI()) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }
            String clientCodeHeader = request.getHeader(AuthClientHeaders.CLIENT_CODE_HEADER);
            if (!StringUtils.hasText(clientCodeHeader)) {
                writeUnauthorized(response, ErrorCodes.AUTH_CLIENT_CODE_MISSING);
                return;
            }
            Optional<String> token = resolveToken(request);
            token.ifPresent(value -> {
                try {
                    UserContext userContext = jwtTokenService.parseAccessToken(value);
                    if (!clientCodeHeader.equalsIgnoreCase(userContext.clientCode())) {
                        throw new IllegalArgumentException("client mismatch");
                    }
                    if (loginSessionService.validateAndTouch(userContext.sessionId(), clientCodeHeader)) {
                        if (!iamService.isApiAllowed(userContext, request.getMethod(), request.getRequestURI())) {
                            throw new SecurityException("forbidden");
                        }
                        UserContextHolder.set(userContext);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userContext,
                                value,
                                List.of(new SimpleGrantedAuthority("ROLE_" + userContext.role().toUpperCase(Locale.ROOT))));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (SecurityException ex) {
                    UserContextHolder.clear();
                    SecurityContextHolder.clearContext();
                    throw ex;
                } catch (IllegalArgumentException ignored) {
                    UserContextHolder.clear();
                    SecurityContextHolder.clearContext();
                }
            });
            if (response.isCommitted()) {
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null
                    && token.isPresent()) {
                writeUnauthorized(response, ErrorCodes.UNAUTHORIZED);
                return;
            }
            filterChain.doFilter(request, response);
        } catch (SecurityException ex) {
            writeForbidden(response);
        } finally {
            UserContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private boolean isPublicPath(String requestUri) {
        return PUBLIC_PATHS.contains(requestUri) || requestUri.startsWith("/api/v1/auth/captcha/");
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        writeUnauthorized(response, ErrorCodes.UNAUTHORIZED, message);
    }

    private void writeUnauthorized(HttpServletResponse response, ErrorCodes errorCode) throws IOException {
        writeUnauthorized(response, errorCode, errorCode.message());
    }

    private void writeUnauthorized(HttpServletResponse response, ErrorCodes errorCode, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(String.valueOf(errorCode.code()), message));
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(
                String.valueOf(ErrorCodes.FORBIDDEN.code()),
                ErrorCodes.FORBIDDEN.message()));
    }
}

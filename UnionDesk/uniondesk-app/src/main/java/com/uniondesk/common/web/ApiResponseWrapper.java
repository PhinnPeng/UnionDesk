package com.uniondesk.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 自动将 Controller 返回的业务对象包装为 {@code ApiResponse<T>} 统一信封结构。
 *
 * <p>包装规则：
 * <ul>
 *   <li>{@code ApiResponse} 子类型 → 原样返回，不二次包装</li>
 *   <li>{@code void} 返回 → 跳过（通常配合 {@code @ResponseStatus} 使用）</li>
 *   <li>{@code String} 返回 → 跳过（避免序列化冲突）</li>
 *   <li>其他业务对象 → 自动包装为 {@code ApiResponse.ok(data)}</li>
 * </ul>
 */
@RestControllerAdvice
public class ApiResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // void 返回跳过
        if (Void.TYPE.equals(returnType.getParameterType())) {
            return false;
        }
        // 已是 ApiResponse 的跳过，不二次包装
        if (ApiResponse.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }
        // String 返回跳过（Spring 会将其作为原始文本处理）
        if (String.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return ApiResponse.ok(null);
        }
        if (body instanceof ApiResponse<?> apiResponse) {
            return apiResponse;
        }
        return ApiResponse.ok(body);
    }
}

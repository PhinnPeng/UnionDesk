package com.uniondesk.common.web;

import com.uniondesk.auth.core.AuthenticationFailedException;
import com.uniondesk.auth.core.AuthCaptchaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;
import java.util.Locale;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailed(AuthenticationFailedException ex) {
        return toResponse(ErrorCodes.AUTH_LOGIN_FAILED);
    }

    @ExceptionHandler(AuthCaptchaException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthCaptcha(AuthCaptchaException ex) {
        return toResponse(ErrorCodes.AUTH_CAPTCHA_FAILED);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(RuntimeException ex) {
        ErrorCodes errorCode = resolveErrorCode(ex.getMessage());
        String message = errorCode == ErrorCodes.BAD_REQUEST && containsCjk(ex.getMessage())
                ? ex.getMessage()
                : errorCode.message();
        return toResponse(errorCode, message);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        return toResponse(ErrorCodes.fromStatus(ex.getStatusCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        return toResponse(ErrorCodes.VALIDATION_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCodes errorCode) {
        return toResponse(errorCode, errorCode.message());
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCodes errorCode, String message) {
        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.error(String.valueOf(errorCode.code()), message));
    }

    private ErrorCodes resolveErrorCode(String message) {
        if (!StringUtils.hasText(message)) {
            return ErrorCodes.BAD_REQUEST;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("not found")) {
            return ErrorCodes.NOT_FOUND;
        }
        if (normalized.contains("unauthorized")) {
            return ErrorCodes.UNAUTHORIZED;
        }
        if (normalized.contains("forbidden")) {
            return ErrorCodes.FORBIDDEN;
        }
        if (normalized.contains("required")) {
            return ErrorCodes.VALIDATION_ERROR;
        }
        if (normalized.contains("captcha")) {
            return ErrorCodes.AUTH_CAPTCHA_FAILED;
        }
        return ErrorCodes.BAD_REQUEST;
    }

    private boolean containsCjk(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            if (ch >= '\u4e00' && ch <= '\u9fff') {
                return true;
            }
        }
        return false;
    }
}

package com.uniondesk.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum ErrorCodes {
    AUTH_LOGIN_FAILED(10001, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    AUTH_CAPTCHA_FAILED(10003, "验证码校验失败", HttpStatus.BAD_REQUEST),
    AUTH_CLIENT_CODE_MISSING(40102, "客户端标识缺失", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(40101, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40301, "无操作权限", HttpStatus.FORBIDDEN),
    NOT_FOUND(40401, "资源不存在", HttpStatus.NOT_FOUND),
    VALIDATION_ERROR(40001, "参数校验失败", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(40002, "请求参数错误", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ErrorCodes(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }

    public static ErrorCodes fromStatus(HttpStatusCode statusCode) {
        if (statusCode == null) {
            return BAD_REQUEST;
        }
        return switch (statusCode.value()) {
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            default -> BAD_REQUEST;
        };
    }
}

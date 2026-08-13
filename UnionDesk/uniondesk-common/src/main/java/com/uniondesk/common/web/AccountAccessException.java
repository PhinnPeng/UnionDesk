package com.uniondesk.common.web;

/**
 * 账号/域访问被禁用的业务异常。
 * <p>用于登录阶段识别「账号级禁用」与「域级全部禁用」，由 {@link ApiExceptionHandler} 统一映射为可读响应，
 * 避免落入数据库外键失败等 50001 内部错误。错误码见 {@link ErrorCodes#AUTH_ACCOUNT_DISABLED} /
 * {@link ErrorCodes#AUTH_NO_ACCESSIBLE_DOMAIN}。
 */
public class AccountAccessException extends RuntimeException {

    private final ErrorCodes errorCode;

    public AccountAccessException(ErrorCodes errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCodes errorCode() {
        return errorCode;
    }
}

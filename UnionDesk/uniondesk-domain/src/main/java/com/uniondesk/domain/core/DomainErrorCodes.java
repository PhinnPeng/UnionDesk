package com.uniondesk.domain.core;

import org.springframework.http.HttpStatus;

/**
 * 业务域模块错误码（foundation-rules DR-01 / DR-02 等）。
 */
public enum DomainErrorCodes {

    /** DR-01：业务域注册配置 = 不允许注册 */
    REGISTRATION_DISALLOWED(41101, "该业务域不允许自助注册", HttpStatus.BAD_REQUEST),

    /** DR-02：业务域邀请配置 = 不允许邀请 */
    INVITATION_DISALLOWED(41102, "该业务域不支持邀请码入域", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus status;

    DomainErrorCodes(int code, String message, HttpStatus status) {
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

    public DomainBusinessException toException() {
        return new DomainBusinessException(this);
    }
}

package com.uniondesk.domain.core;

/**
 * 业务域模块可预期业务异常，携带 {@link DomainErrorCodes}。
 */
public class DomainBusinessException extends RuntimeException {

    private final DomainErrorCodes errorCode;

    public DomainBusinessException(DomainErrorCodes errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public DomainErrorCodes errorCode() {
        return errorCode;
    }
}

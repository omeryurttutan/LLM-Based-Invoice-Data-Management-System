package com.faturaocr.domain.common.exception;

import lombok.Getter;

/**
 * Base exception for all domain-level exceptions.
 */
@Getter
public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public DomainException(String message) {
        super(message);
        this.errorCode = "DOMAIN_ERROR";
    }

    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

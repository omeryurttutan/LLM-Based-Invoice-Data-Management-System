package com.faturaocr.interfaces.rest.common;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standard error response structure.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String errorCode; // Kept for backward compatibility if used, mapped to code
    private String message;
    private String path;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String code; // Custom error code like INVOICE_DUPLICATE_DETECTED
    private Map<String, Object> details; // For extra data like duplicate lists

    private List<FieldError> fieldErrors;

    // Backward compatible constructor
    public ErrorResponse(String errorCode, String message, String path) {
        this.errorCode = errorCode;
        this.code = errorCode;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    // Backward compatible constructor
    public ErrorResponse(String errorCode, String message, String path, List<FieldError> fieldErrors) {
        this(errorCode, message, path);
        this.fieldErrors = fieldErrors;
    }

    /**
     * Represents a field-level validation error.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}

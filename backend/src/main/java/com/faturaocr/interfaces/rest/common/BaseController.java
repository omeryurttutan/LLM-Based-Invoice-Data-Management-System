package com.faturaocr.interfaces.rest.common;

import org.springframework.http.ResponseEntity;

/**
 * Base controller with common helper methods.
 */
public abstract class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(201).body(ApiResponse.success(data));
    }

    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}

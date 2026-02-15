package com.faturaocr.interfaces.rest.common;

import com.faturaocr.application.common.exception.BusinessException;
import com.faturaocr.application.common.exception.DuplicateInvoiceException;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.common.exception.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
                        EntityNotFoundException ex, WebRequest request) {

                LOGGER.warn("Entity not found: {}", ex.getMessage());

                ErrorResponse error = new ErrorResponse(
                                ex.getErrorCode(),
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(DomainException.class)
        public ResponseEntity<ErrorResponse> handleDomainException(
                        DomainException ex, WebRequest request) {

                LOGGER.warn("Domain exception: {}", ex.getMessage());

                ErrorResponse error = new ErrorResponse(
                                ex.getErrorCode(),
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(DuplicateInvoiceException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateInvoiceException(
                        DuplicateInvoiceException ex, HttpServletRequest request) {
                log.warn("Duplicate invoice detected: {}", ex.getMessage());

                Map<String, Object> details = new LinkedHashMap<>();
                details.put("hasDuplicates", ex.getDuplicateCheckResult().isHasDuplicates());
                details.put("highestConfidence", ex.getDuplicateCheckResult().getHighestConfidence());
                details.put("duplicates", ex.getDuplicateCheckResult().getDuplicates());

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error(HttpStatus.CONFLICT.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .details(details)
                                .code(ex.getErrorCode())
                                .build();

                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
                log.warn("Business exception: {}", ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .code(ex.getErrorCode())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        MethodArgumentNotValidException ex, WebRequest request) {

                List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> new ErrorResponse.FieldError(
                                                error.getField(),
                                                error.getDefaultMessage(),
                                                error.getRejectedValue()))
                                .collect(Collectors.toList());

                ErrorResponse error = new ErrorResponse(
                                "VALIDATION_ERROR",
                                "Validation failed for one or more fields",
                                request.getDescription(false).replace("uri=", ""),
                                fieldErrors);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
                        IllegalArgumentException ex, WebRequest request) {

                LOGGER.warn("Illegal argument: {}", ex.getMessage());

                ErrorResponse error = new ErrorResponse(
                                "INVALID_ARGUMENT",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(
                        org.springframework.security.access.AccessDeniedException ex, WebRequest request) {

                LOGGER.warn("Access denied: {}", ex.getMessage());

                ErrorResponse error = new ErrorResponse(
                                "ACCESS_DENIED",
                                "You do not have permission to access this resource",
                                request.getDescription(false).replace("uri=", ""));

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleAllUncaughtException(
                        Exception ex, WebRequest request) {

                LOGGER.error("Unexpected error occurred", ex);

                ErrorResponse error = new ErrorResponse(
                                "INTERNAL_ERROR",
                                "An unexpected error occurred. Please try again later.",
                                request.getDescription(false).replace("uri=", ""));

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}

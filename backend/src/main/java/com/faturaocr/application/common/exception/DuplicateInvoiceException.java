package com.faturaocr.application.common.exception;

import com.faturaocr.application.invoice.dto.DuplicateCheckResult;
import lombok.Getter;

/**
 * Thrown when a potential duplicate invoice is detected during creation.
 * Carries the DuplicateCheckResult for structured 409 response.
 */
@Getter
public class DuplicateInvoiceException extends BusinessException {

    private final DuplicateCheckResult duplicateCheckResult;

    public DuplicateInvoiceException(DuplicateCheckResult result) {
        super("INVOICE_DUPLICATE_DETECTED",
                "Potential duplicate invoice(s) detected. Use forceDuplicate=true to create anyway.");
        this.duplicateCheckResult = result;
    }
}

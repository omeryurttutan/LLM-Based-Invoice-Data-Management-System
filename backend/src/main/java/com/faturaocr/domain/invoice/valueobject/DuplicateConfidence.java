package com.faturaocr.domain.invoice.valueobject;

/**
 * Confidence level for duplicate invoice detection.
 */
public enum DuplicateConfidence {
    HIGH, // Exact invoice number match
    MEDIUM, // Same supplier tax number + date + amount
    LOW, // Fuzzy supplier name + date + similar amount
    NONE // No duplicates found
}

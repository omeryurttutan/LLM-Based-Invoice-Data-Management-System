package com.faturaocr.domain.common.valueobject;

/**
 * Marker interface for Value Objects.
 * Value Objects are immutable and compared by their attributes, not identity.
 */
public interface ValueObject {
    // Value objects must implement equals() and hashCode()
    // based on their attributes, not identity
}

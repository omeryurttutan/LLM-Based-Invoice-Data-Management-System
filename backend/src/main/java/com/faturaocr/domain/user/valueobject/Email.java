package com.faturaocr.domain.user.valueobject;

import com.faturaocr.domain.common.valueobject.ValueObject;

import java.util.Objects;
import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value Object representing an email address.
 */
@Getter
@EqualsAndHashCode
public final class Email implements ValueObject {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String value) {
        Objects.requireNonNull(value, "Email cannot be null");
        String trimmed = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        return new Email(trimmed);
    }

    @Override
    public String toString() {
        return value;
    }
}

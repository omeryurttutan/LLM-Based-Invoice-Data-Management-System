package com.faturaocr.infrastructure.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizationUtilsTest {

    @Test
    @DisplayName("Should sanitize HTML")
    void shouldSanitizeHtml() {
        String input = "<script>alert('xss')</script>Hello";
        String result = SanitizationUtils.sanitizeHtml(input);
        // The implementation only strips tags, it does not remove content inside tags
        assertThat(result).isEqualTo("alert('xss')Hello");
    }

    @Test
    @DisplayName("Should return null for null input in HTML sanitization")
    void shouldReturnNullForNullHtml() {
        assertThat(SanitizationUtils.sanitizeHtml(null)).isNull();
    }

    @Test
    @DisplayName("Should sanitize log input")
    void shouldSanitizeLog() {
        String input = "Hello\nWorld\tTab";
        String result = SanitizationUtils.sanitizeForLog(input);
        assertThat(result).isEqualTo("Hello_World_Tab");
    }

    @Test
    @DisplayName("Should return null for null input in log sanitization")
    void shouldReturnNullForNullLog() {
        assertThat(SanitizationUtils.sanitizeForLog(null)).isNull();
    }
}

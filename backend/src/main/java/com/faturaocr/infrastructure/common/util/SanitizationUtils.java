package com.faturaocr.infrastructure.common.util;

public class SanitizationUtils {

    private SanitizationUtils() {
        // Private constructor
    }

    /**
     * Strip HTML tags from input.
     */
    public static String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        // Simple HTML tag stripping
        return input.replaceAll("<[^>]*>", "");
    }

    /**
     * Sanitize input for logging to prevent log injection.
     */
    public static String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }
        // Remove newlines and tabs
        return input.replace('\n', '_').replace('\r', '_').replace('\t', '_');
    }
}

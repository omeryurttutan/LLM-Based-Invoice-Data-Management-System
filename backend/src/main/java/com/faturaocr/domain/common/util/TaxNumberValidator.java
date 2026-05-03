package com.faturaocr.domain.common.util;

/**
 * Turkish Tax Number (Vergi Kimlik Numarası - VKN) validator.
 * Validates 10-digit corporate tax numbers using the official algorithm
 * published by Turkey's Revenue Administration (GİB).
 */
public final class TaxNumberValidator {

    private TaxNumberValidator() {
        // Utility class
    }

    /**
     * Validates a Turkish corporate tax number (VKN - 10 digits).
     * 
     * Algorithm:
     * 1. The VKN must be exactly 10 digits.
     * 2. For each of the first 9 digits:
     *    a. Add the digit to its position (1-indexed), take mod 10.
     *    b. Multiply the result by 2^(position), take mod 9. If the result is 0, use 9.
     * 3. Sum all 9 intermediate results, take mod 10.
     * 4. Subtract from 10, take mod 10 → this should equal the 10th digit (check digit).
     *
     * @param taxNumber The tax number string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidVKN(String taxNumber) {
        if (taxNumber == null || !taxNumber.matches("^\\d{10}$")) {
            return false;
        }

        int[] digits = new int[10];
        for (int i = 0; i < 10; i++) {
            digits[i] = taxNumber.charAt(i) - '0';
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int tmp = (digits[i] + (9 - i)) % 10;
            int power = (int) Math.pow(2, (9 - i));
            int result = (tmp * power) % 9;
            if (tmp != 0 && result == 0) {
                result = 9;
            }
            sum += result;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == digits[9];
    }

    /**
     * Validates a Turkish personal identity number (TCKN - 11 digits).
     * 
     * @param tckn The identity number string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidTCKN(String tckn) {
        if (tckn == null || !tckn.matches("^\\d{11}$")) {
            return false;
        }

        // First digit cannot be 0
        if (tckn.charAt(0) == '0') {
            return false;
        }

        int[] digits = new int[11];
        for (int i = 0; i < 11; i++) {
            digits[i] = tckn.charAt(i) - '0';
        }

        // 10th digit check: ((d1+d3+d5+d7+d9)*7 - (d2+d4+d6+d8)) % 10 == d10
        int oddSum = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
        int evenSum = digits[1] + digits[3] + digits[5] + digits[7];
        int tenthDigit = ((oddSum * 7) - evenSum) % 10;
        if (tenthDigit < 0) tenthDigit += 10;
        if (tenthDigit != digits[9]) {
            return false;
        }

        // 11th digit check: (d1+d2+d3+...+d10) % 10 == d11
        int totalSum = 0;
        for (int i = 0; i < 10; i++) {
            totalSum += digits[i];
        }
        return (totalSum % 10) == digits[10];
    }
}

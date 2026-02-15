package com.faturaocr.infrastructure.export.accounting;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class AccountingExportUtils {

    private static final Locale TURKISH_LOCALE = new Locale("tr", "TR");
    private static final Charset WINDOWS_1254 = Charset.forName("windows-1254");

    public String formatDate(LocalDate date, String pattern) {
        if (date == null)
            return "";
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public String formatNumber(BigDecimal number, boolean useDotDecimal) {
        if (number == null)
            return "0";

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(TURKISH_LOCALE);
        if (useDotDecimal) {
            symbols.setDecimalSeparator('.');
            symbols.setGroupingSeparator(',');
        } else {
            symbols.setDecimalSeparator(',');
            symbols.setGroupingSeparator('.');
        }

        DecimalFormat df = new DecimalFormat("###,##0.00", symbols);
        return df.format(number);
    }

    public String sanitizeText(String text, int maxLength) {
        if (text == null)
            return "";
        String sanitized = text.trim();
        if (sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }
        return sanitized;
    }

    public String translateCurrency(String currencyCode) {
        if (currencyCode == null)
            return "TL";
        switch (currencyCode.toUpperCase()) {
            case "USD":
                return "USD";
            case "EUR":
                return "EUR";
            case "GBP":
                return "GBP";
            default:
                return "TL";
        }
    }

    public String translateCurrencyForLogo(String currencyCode) {
        if (currencyCode == null)
            return "0"; // TL
        switch (currencyCode.toUpperCase()) {
            case "USD":
                return "1";
            case "EUR":
                return "20";
            case "GBP":
                return "17"; // Logo standard might vary, using common codes
            case "TRY":
            case "TL":
                return "0";
            default:
                return "0";
        }
    }

    public byte[] encodeForWindows1254(String text) {
        return text.getBytes(WINDOWS_1254);
    }
}

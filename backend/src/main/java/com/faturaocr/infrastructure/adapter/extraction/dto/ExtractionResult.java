package com.faturaocr.infrastructure.adapter.extraction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ExtractionResult {
    @JsonProperty("data")
    private InvoiceData invoiceData;

    @JsonProperty("confidence_score")
    private BigDecimal confidenceScore;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("validation_result")
    private ValidationResult validationResult;

    @Data
    public static class InvoiceData {
        @JsonProperty("fatura_no")
        private String invoiceNumber;

        @JsonProperty("tarih")
        private LocalDate invoiceDate;

        @JsonProperty("vade_tarihi")
        private LocalDate dueDate;

        @JsonProperty("gonderici_unvan")
        private String supplierName;

        @JsonProperty("gonderici_vkn")
        private String supplierTaxId;

        @JsonProperty("gonderici_adres")
        private String supplierAddress;

        @JsonProperty("alici_unvan")
        private String buyerName;

        @JsonProperty("alici_vkn")
        private String buyerTaxNumber;

        @JsonProperty("kalemler")
        private List<InvoiceItemData> items;

        @JsonProperty("ara_toplam")
        private BigDecimal subtotal;

        @JsonProperty("vergi_toplam")
        private BigDecimal taxAmount;

        @JsonProperty("genel_toplam")
        private BigDecimal totalAmount;

        @JsonProperty("para_birimi")
        private String currency;

        @JsonProperty("notlar")
        private String notes;
    }

    @Data
    public static class InvoiceItemData {
        @JsonProperty("aciklama")
        private String description;

        @JsonProperty("miktar")
        private BigDecimal quantity;

        @JsonProperty("birim")
        private String unit;

        @JsonProperty("birim_fiyat")
        private BigDecimal unitPrice;

        @JsonProperty("toplam_tutar")
        private BigDecimal lineTotal;

        @JsonProperty("kdv_orani")
        private BigDecimal taxRate;

        @JsonProperty("kdv_tutari")
        private BigDecimal taxAmount;
    }

    @Data
    public static class ValidationResult {
        @JsonProperty("is_valid")
        private boolean isValid;

        @JsonProperty("errors")
        private List<String> errors;
    }
}

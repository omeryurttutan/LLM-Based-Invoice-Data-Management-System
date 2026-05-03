package com.faturaocr.infrastructure.persistence.dashboard;

import com.faturaocr.application.dashboard.dto.CategoryDistributionResponse;
import com.faturaocr.application.dashboard.dto.DashboardStatsResponse;
import com.faturaocr.application.dashboard.dto.ExtractionPerformanceResponse;
import com.faturaocr.application.dashboard.dto.MonthlyTrendResponse;
import com.faturaocr.application.dashboard.dto.PendingActionsResponse;
import com.faturaocr.application.dashboard.dto.TopSuppliersResponse;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.UUID;

public class DashboardRowMappers {

    public static RowMapper<DashboardStatsResponse.Summary> summaryMapper() {
        return (rs, rowNum) -> DashboardStatsResponse.Summary.builder()
                .totalInvoices(rs.getInt("total_count"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .averageAmount(rs.getBigDecimal("average_amount"))
                .pendingCount(rs.getInt("pending_count"))
                .pendingAmount(rs.getBigDecimal("pending_amount"))
                .verifiedCount(rs.getInt("verified_count"))
                .verifiedAmount(rs.getBigDecimal("verified_amount"))
                .rejectedCount(rs.getInt("rejected_count"))
                .processingCount(rs.getInt("processing_count"))
                .build();
    }

    public static RowMapper<DashboardStatsResponse.ConfidenceStats> confidenceStatsMapper() {
        return (rs, rowNum) -> DashboardStatsResponse.ConfidenceStats.builder()
                .averageScore(rs.getBigDecimal("avg_score"))
                .highConfidence(rs.getInt("high_conf"))
                .mediumConfidence(rs.getInt("medium_conf"))
                .lowConfidence(rs.getInt("low_conf"))
                .build();
    }

    public static RowMapper<CategoryDistributionResponse> categoryDistributionMapper() {
        return (rs, rowNum) -> {
            String catName = rs.getString("category_name");
            String catColor = rs.getString("category_color");
            UUID catId = rs.getObject("category_id", UUID.class);

            return CategoryDistributionResponse.builder()
                    .categoryId(catId)
                    .categoryName(catName != null ? catName : "Kategorisiz")
                    .categoryColor(catColor != null ? catColor : "#9CA3AF")
                    .invoiceCount(rs.getInt("invoice_count"))
                    .totalAmount(rs.getBigDecimal("total_amount"))
                    .build();
        };
    }

    public static RowMapper<MonthlyTrendResponse> monthlyTrendMapper() {
        return (rs, rowNum) -> MonthlyTrendResponse.builder()
                .month(rs.getString("month_key"))
                .invoiceCount(rs.getInt("invoice_count"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .verifiedAmount(rs.getBigDecimal("verified_amount"))
                .averageAmount(rs.getBigDecimal("average_amount"))
                .build();
    }

    public static RowMapper<TopSuppliersResponse.SupplierStats> supplierStatsMapper() {
        return (rs, rowNum) -> TopSuppliersResponse.SupplierStats.builder()
                .supplierName(rs.getString("supplier_name"))
                .supplierTaxNumber(rs.getString("supplier_tax_number"))
                .invoiceCount(rs.getInt("invoice_count"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .build();
    }

    public static RowMapper<PendingActionsResponse.PendingInvoice> pendingInvoiceMapper() {
        return (rs, rowNum) -> {
            java.sql.Timestamp ts = rs.getTimestamp("created_at");
            LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;
            long daysPending = 0;
            if (createdAt != null) {
                daysPending = java.time.temporal.ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
            }

            return PendingActionsResponse.PendingInvoice.builder()
                    .id(rs.getObject("id", UUID.class))
                    .invoiceNumber(rs.getString("invoice_number"))
                    .supplierName(rs.getString("supplier_name"))
                    .totalAmount(rs.getBigDecimal("total_amount"))
                    .currency(Currency.valueOf(rs.getString("currency")))
                    .sourceType(SourceType.valueOf(rs.getString("source_type")))
                    .confidenceScore(rs.getBigDecimal("confidence_score"))
                    .createdAt(createdAt)
                    .daysPending(daysPending)
                    .build();
        };
    }

    public static RowMapper<ExtractionPerformanceResponse.ProviderStats> providerStatsMapper() {
        return (rs, rowNum) -> {
            String providerStr = rs.getString("llm_provider");
            LlmProvider provider = providerStr != null ? LlmProvider.valueOf(providerStr) : null;

            return ExtractionPerformanceResponse.ProviderStats.builder()
                    .provider(provider)
                    .attempts(rs.getInt("attempts"))
                    .successCount(rs.getInt("success_count"))
                    .failureCount(rs.getInt("failure_count"))
                    .averageConfidence(rs.getBigDecimal("avg_confidence"))
                    .fallbackCount(0)
                    .build();
        };
    }
}

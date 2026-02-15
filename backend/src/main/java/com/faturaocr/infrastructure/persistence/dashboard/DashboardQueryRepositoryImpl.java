package com.faturaocr.infrastructure.persistence.dashboard;

import com.faturaocr.application.dashboard.dto.*;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DashboardQueryRepositoryImpl implements DashboardQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public DashboardStatsResponse getDashboardStats(UUID companyId, LocalDate dateFrom, LocalDate dateTo,
            Currency currency) {
        long startTime = System.currentTimeMillis();

        // Base where clause for all queries
        String baseWhere = "WHERE company_id = ? AND is_deleted = false";
        List<Object> params = new ArrayList<>();
        params.add(companyId);

        if (dateFrom != null) {
            baseWhere += " AND invoice_date >= ?";
            params.add(dateFrom);
        }
        if (dateTo != null) {
            baseWhere += " AND invoice_date <= ?";
            params.add(dateTo);
        }

        // Summary Statistics (Counts and Amounts)
        String summarySql = String.format(
                """
                        SELECT
                            COUNT(*) as total_count,
                            COALESCE(SUM(CASE WHEN currency = ? THEN total_amount ELSE 0 END), 0) as total_amount,
                            COALESCE(AVG(CASE WHEN currency = ? THEN total_amount END), 0) as average_amount,
                            COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_count,
                            COALESCE(SUM(CASE WHEN status = 'PENDING' AND currency = ? THEN total_amount ELSE 0 END), 0) as pending_amount,
                            COUNT(CASE WHEN status = 'VERIFIED' THEN 1 END) as verified_count,
                            COALESCE(SUM(CASE WHEN status = 'VERIFIED' AND currency = ? THEN total_amount ELSE 0 END), 0) as verified_amount,
                            COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected_count,
                            COUNT(CASE WHEN status = 'PROCESSING' THEN 1 END) as processing_count
                        FROM invoices
                        %s
                        """,
                baseWhere);

        List<Object> summaryParams = new ArrayList<>();
        // Add currency params 4 times for the CASE WHEN currency checks
        String currencyStr = currency != null ? currency.name() : "TRY";
        summaryParams.add(currencyStr);
        summaryParams.add(currencyStr);
        summaryParams.add(currencyStr);
        summaryParams.add(currencyStr);
        summaryParams.addAll(params);

        DashboardStatsResponse.Summary summary = jdbcTemplate.queryForObject(summarySql,
                (rs, rowNum) -> DashboardStatsResponse.Summary.builder()
                        .totalInvoices(rs.getInt("total_count"))
                        .totalAmount(rs.getBigDecimal("total_amount"))
                        .averageAmount(rs.getBigDecimal("average_amount"))
                        .pendingCount(rs.getInt("pending_count"))
                        .pendingAmount(rs.getBigDecimal("pending_amount"))
                        .verifiedCount(rs.getInt("verified_count"))
                        .verifiedAmount(rs.getBigDecimal("verified_amount"))
                        .rejectedCount(rs.getInt("rejected_count"))
                        .processingCount(rs.getInt("processing_count"))
                        .build(),
                summaryParams.toArray());

        // Source Breakdown
        String sourceSql = String.format("""
                SELECT source_type, COUNT(*) as count
                FROM invoices
                %s
                GROUP BY source_type
                """, baseWhere);

        Map<String, DashboardStatsResponse.SourceStats> sourceStats = new HashMap<>();
        List<Map<String, Object>> sourceRows = jdbcTemplate.queryForList(sourceSql, params.toArray());

        int totalInvoices = summary != null ? summary.getTotalInvoices() : 0;

        for (Map<String, Object> row : sourceRows) {
            String source = (String) row.get("source_type");
            int count = ((Number) row.get("count")).intValue();
            BigDecimal percentage = totalInvoices > 0
                    ? BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalInvoices), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            sourceStats.put(source, DashboardStatsResponse.SourceStats.builder()
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        // Confidence Stats (only for LLM source type)
        // Need to check if date range params were added to base params, we need to copy
        // them
        List<Object> confidenceParams = new ArrayList<>(params);

        String confidenceSql = String.format("""
                SELECT
                    COALESCE(AVG(confidence_score), 0) as avg_score,
                    COUNT(CASE WHEN confidence_score >= 90 THEN 1 END) as high_conf,
                    COUNT(CASE WHEN confidence_score >= 70 AND confidence_score < 90 THEN 1 END) as medium_conf,
                    COUNT(CASE WHEN confidence_score < 70 THEN 1 END) as low_conf
                FROM invoices
                %s AND source_type = 'LLM'
                """, baseWhere);

        DashboardStatsResponse.ConfidenceStats confidenceStats = jdbcTemplate.queryForObject(confidenceSql,
                (rs, rowNum) -> DashboardStatsResponse.ConfidenceStats.builder()
                        .averageScore(rs.getBigDecimal("avg_score"))
                        .highConfidence(rs.getInt("high_conf"))
                        .mediumConfidence(rs.getInt("medium_conf"))
                        .lowConfidence(rs.getInt("low_conf"))
                        .build(),
                confidenceParams.toArray());

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 300) {
            log.warn("Slow dashboard stats query: {} ms", duration);
        }

        return DashboardStatsResponse.builder()
                .period(DashboardStatsResponse.Period.builder()
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .currency(currency)
                        .build())
                .summary(summary)
                .sourceBreakdown(sourceStats)
                .confidenceStats(confidenceStats)
                .build();
    }

    @Override
    public List<CategoryDistributionResponse> getCategoryDistribution(UUID companyId, LocalDate dateFrom,
            LocalDate dateTo, Currency currency) {
        long startTime = System.currentTimeMillis();

        String sql = """
                SELECT
                    i.category_id,
                    c.name as category_name,
                    c.color as category_color,
                    COUNT(*) as invoice_count,
                    SUM(i.total_amount) as total_amount
                FROM invoices i
                LEFT JOIN categories c ON i.category_id = c.id
                WHERE i.company_id = ? AND i.is_deleted = false AND i.currency = ?
                """;

        List<Object> params = new ArrayList<>();
        params.add(companyId);
        params.add(currency != null ? currency.name() : "TRY");

        if (dateFrom != null) {
            sql += " AND i.invoice_date >= ?";
            params.add(dateFrom);
        }
        if (dateTo != null) {
            sql += " AND i.invoice_date <= ?";
            params.add(dateTo);
        }

        sql += " GROUP BY i.category_id, c.name, c.color ORDER BY total_amount DESC";

        List<CategoryDistributionResponse> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
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
        }, params.toArray());

        // Calculate percentages and aggregate "Others" if needed
        // For simplicity in this iteration, we calculate percentages in Java
        BigDecimal grandTotal = results.stream()
                .map(CategoryDistributionResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            results.forEach(r -> r.setPercentage(
                    r.getTotalAmount().multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2,
                            java.math.RoundingMode.HALF_UP)));
        } else {
            results.forEach(r -> r.setPercentage(BigDecimal.ZERO));
        }

        // Limit to top 10 and aggregate others
        if (results.size() > 10) {
            List<CategoryDistributionResponse> top10 = new ArrayList<>(results.subList(0, 10));
            List<CategoryDistributionResponse> others = results.subList(10, results.size());

            CategoryDistributionResponse otherCat = CategoryDistributionResponse.builder()
                    .categoryName("Diğer")
                    .categoryColor("#6B7280")
                    .invoiceCount(others.stream().mapToInt(CategoryDistributionResponse::getInvoiceCount).sum())
                    .totalAmount(others.stream().map(CategoryDistributionResponse::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .build();

            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                otherCat.setPercentage(otherCat.getTotalAmount().multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2,
                        java.math.RoundingMode.HALF_UP));
            } else {
                otherCat.setPercentage(BigDecimal.ZERO);
            }

            top10.add(otherCat);
            results = top10;
        }

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 300) {
            log.warn("Slow category distribution query: {} ms", duration);
        }

        return results;
    }

    @Override
    public List<MonthlyTrendResponse> getMonthlyTrend(UUID companyId, int months, Currency currency) {
        long startTime = System.currentTimeMillis();

        // Target last N months
        LocalDate endDate = LocalDate.now().withDayOfMonth(1);
        LocalDate startDate = endDate.minusMonths(months - 1);

        String sql = """
                SELECT
                    TO_CHAR(invoice_date, 'YYYY-MM') as month_key,
                    COUNT(*) as invoice_count,
                    SUM(total_amount) as total_amount,
                    SUM(CASE WHEN status = 'VERIFIED' THEN total_amount ELSE 0 END) as verified_amount,
                    AVG(total_amount) as average_amount
                FROM invoices
                WHERE company_id = ? AND is_deleted = false AND currency = ?
                AND invoice_date >= ?
                GROUP BY TO_CHAR(invoice_date, 'YYYY-MM')
                ORDER BY month_key DESC
                """;

        List<Object> params = new ArrayList<>();
        params.add(companyId);
        params.add(currency != null ? currency.name() : "TRY");
        params.add(startDate);

        Map<String, MonthlyTrendResponse> dataMap = new HashMap<>();

        jdbcTemplate.query(sql, (rs) -> {
            String monthKey = rs.getString("month_key");
            dataMap.put(monthKey, MonthlyTrendResponse.builder()
                    .month(monthKey)
                    // Label will be set later
                    .invoiceCount(rs.getInt("invoice_count"))
                    .totalAmount(rs.getBigDecimal("total_amount"))
                    .verifiedAmount(rs.getBigDecimal("verified_amount"))
                    .averageAmount(rs.getBigDecimal("average_amount"))
                    .build());
        }, params.toArray());

        // Fill gaps
        List<MonthlyTrendResponse> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        String[] turkishMonths = { "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül",
                "Ekim", "Kasım", "Aralık" };

        for (int i = 0; i < months; i++) {
            LocalDate d = endDate.minusMonths(i);
            String ke = d.format(formatter);
            String label = turkishMonths[d.getMonthValue() - 1] + " " + d.getYear();

            MonthlyTrendResponse item = dataMap.getOrDefault(ke, MonthlyTrendResponse.builder()
                    .month(ke)
                    .invoiceCount(0)
                    .totalAmount(BigDecimal.ZERO)
                    .verifiedAmount(BigDecimal.ZERO)
                    .averageAmount(BigDecimal.ZERO)
                    .build());
            item.setLabel(label);
            result.add(item);
        }

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 300) {
            log.warn("Slow monthly trend query: {} ms", duration);
        }

        return result;
    }

    @Override
    public TopSuppliersResponse getTopSuppliers(UUID companyId, LocalDate dateFrom, LocalDate dateTo, Currency currency,
            int limit) {
        long startTime = System.currentTimeMillis();

        String sql = """
                SELECT
                    supplier_name,
                    supplier_tax_number,
                    COUNT(*) as invoice_count,
                    SUM(total_amount) as total_amount
                FROM invoices
                WHERE company_id = ? AND is_deleted = false AND currency = ?
                """;

        List<Object> params = new ArrayList<>();
        params.add(companyId);
        params.add(currency != null ? currency.name() : "TRY");

        if (dateFrom != null) {
            sql += " AND invoice_date >= ?";
            params.add(dateFrom);
        }
        if (dateTo != null) {
            sql += " AND invoice_date <= ?";
            params.add(dateTo);
        }

        sql += " GROUP BY supplier_name, supplier_tax_number ORDER BY total_amount DESC";

        List<TopSuppliersResponse.SupplierStats> allSuppliers = jdbcTemplate.query(sql,
                (rs, rowNum) -> TopSuppliersResponse.SupplierStats.builder()
                        .supplierName(rs.getString("supplier_name"))
                        .supplierTaxNumber(rs.getString("supplier_tax_number"))
                        .invoiceCount(rs.getInt("invoice_count"))
                        .totalAmount(rs.getBigDecimal("total_amount"))
                        .build(),
                params.toArray());

        BigDecimal totalAll = allSuppliers.stream()
                .map(TopSuppliersResponse.SupplierStats::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate percentages
        if (totalAll.compareTo(BigDecimal.ZERO) > 0) {
            allSuppliers.forEach(s -> s.setPercentage(
                    s.getTotalAmount().multiply(BigDecimal.valueOf(100)).divide(totalAll, 2,
                            java.math.RoundingMode.HALF_UP)));
        } else {
            allSuppliers.forEach(s -> s.setPercentage(BigDecimal.ZERO));
        }

        // Split top N and others
        List<TopSuppliersResponse.SupplierStats> topSuppliers = new ArrayList<>();
        int othersCount = 0;
        BigDecimal othersAmount = BigDecimal.ZERO;

        if (allSuppliers.size() > limit) {
            topSuppliers.addAll(allSuppliers.subList(0, limit));
            List<TopSuppliersResponse.SupplierStats> others = allSuppliers.subList(limit, allSuppliers.size());
            othersCount = others.stream().mapToInt(TopSuppliersResponse.SupplierStats::getInvoiceCount).sum();
            othersAmount = others.stream().map(TopSuppliersResponse.SupplierStats::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            topSuppliers.addAll(allSuppliers);
        }

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 300) {
            log.warn("Slow top suppliers query: {} ms", duration);
        }

        return TopSuppliersResponse.builder()
                .suppliers(topSuppliers)
                .othersCount(othersCount)
                .othersAmount(othersAmount)
                .build();
    }

    @Override
    public PendingActionsResponse getPendingActions(UUID companyId, int limit) {
        long startTime = System.currentTimeMillis();

        // Count all pending
        String countSql = "SELECT COUNT(*) FROM invoices WHERE company_id = ? AND is_deleted = false AND status = 'PENDING'";
        Integer totalPending = jdbcTemplate.queryForObject(countSql, Integer.class, companyId);
        if (totalPending == null)
            totalPending = 0;

        // Get top pending (low confidence first, then oldest)
        String sql = """
                SELECT
                    id, invoice_number, supplier_name, total_amount, currency,
                    source_type, confidence_score, created_at
                FROM invoices
                WHERE company_id = ? AND is_deleted = false AND status = 'PENDING'
                ORDER BY
                    CASE WHEN confidence_score IS NULL THEN 1 ELSE 0 END,
                    confidence_score ASC,
                    created_at ASC
                LIMIT ?
                """;

        List<PendingActionsResponse.PendingInvoice> invoices = jdbcTemplate.query(sql, (rs, rowNum) -> {
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
        }, companyId, limit);

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 300) {
            log.warn("Slow pending actions query: {} ms", duration);
        }

        return PendingActionsResponse.builder()
                .totalPending(totalPending)
                .invoices(invoices)
                .build();
    }

    @Override
    public List<StatusTimelineResponse> getStatusTimeline(UUID companyId, int days) {
        // Implementation for status timeline
        // Since we need to query based on created_at, verified_at, rejected_at which
        // are different columns,
        // we might need separate queries or a UNION

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        String sql = """
                SELECT d::date as date_key,
                    (SELECT COUNT(*) FROM invoices WHERE company_id = ? AND is_deleted = false AND DATE(created_at) = d::date) as created_count,
                    (SELECT COUNT(*) FROM invoices WHERE company_id = ? AND is_deleted = false AND DATE(verified_at) = d::date) as verified_count,
                    (SELECT COUNT(*) FROM invoices WHERE company_id = ? AND is_deleted = false AND DATE(rejected_at) = d::date) as rejected_count
                FROM generate_series(?, ?, '1 day'::interval) d
                ORDER BY d DESC
                """;

        // Note: generate_series is postgres specific. If using H2 for tests, might need
        // adjustment or Java-side filling.
        // For portability, let's do Java-side filling instead of complex SQL with
        // generate_series

        String createdSql = "SELECT DATE(created_at) as d, COUNT(*) as c FROM invoices WHERE company_id = ? AND is_deleted = false AND created_at >= ? GROUP BY DATE(created_at)";
        String verifiedSql = "SELECT DATE(verified_at) as d, COUNT(*) as c FROM invoices WHERE company_id = ? AND is_deleted = false AND verified_at >= ? GROUP BY DATE(verified_at)";
        String rejectedSql = "SELECT DATE(rejected_at) as d, COUNT(*) as c FROM invoices WHERE company_id = ? AND is_deleted = false AND rejected_at >= ? GROUP BY DATE(rejected_at)";

        LocalDateTime startDateTime = startDate.atStartOfDay();

        Map<LocalDate, Integer> createdMap = queryDateCounts(createdSql, companyId, startDateTime);
        Map<LocalDate, Integer> verifiedMap = queryDateCounts(verifiedSql, companyId, startDateTime);
        Map<LocalDate, Integer> rejectedMap = queryDateCounts(rejectedSql, companyId, startDateTime);

        List<StatusTimelineResponse> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = endDate.minusDays(i);
            result.add(StatusTimelineResponse.builder()
                    .date(d)
                    .created(createdMap.getOrDefault(d, 0))
                    .verified(verifiedMap.getOrDefault(d, 0))
                    .rejected(rejectedMap.getOrDefault(d, 0))
                    .build());
        }

        return result;
    }

    private Map<LocalDate, Integer> queryDateCounts(String sql, UUID companyId, LocalDateTime startDate) {
        Map<LocalDate, Integer> map = new HashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            java.sql.Date d = rs.getDate("d");
            if (d != null) {
                map.put(d.toLocalDate(), rs.getInt("c"));
            }
        }, companyId, startDate);
        return map;
    }

    @Override
    public ExtractionPerformanceResponse getExtractionPerformance(UUID companyId, LocalDate dateFrom,
            LocalDate dateTo) {
        long startTime = System.currentTimeMillis();

        String baseWhere = "WHERE company_id = ? AND is_deleted = false AND source_type = 'LLM'";
        List<Object> params = new ArrayList<>();
        params.add(companyId);

        if (dateFrom != null) {
            baseWhere += " AND invoice_date >= ?";
            params.add(dateFrom);
        }
        if (dateTo != null) {
            baseWhere += " AND invoice_date <= ?";
            params.add(dateTo);
        }

        // Provider Stats
        String providerSql = String.format("""
                SELECT
                    llm_provider,
                    COUNT(*) as attempts,
                    COUNT(CASE WHEN status != 'FAILED' THEN 1 END) as success_count,
                    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failure_count,
                    AVG(confidence_score) as avg_confidence
                FROM invoices
                %s
                GROUP BY llm_provider
                """, baseWhere);

        List<ExtractionPerformanceResponse.ProviderStats> providerStats = jdbcTemplate.query(providerSql,
                (rs, rowNum) -> {
                    String providerStr = rs.getString("llm_provider");
                    LlmProvider provider = providerStr != null ? LlmProvider.valueOf(providerStr) : null;

                    return ExtractionPerformanceResponse.ProviderStats.builder()
                            .provider(provider)
                            .attempts(rs.getInt("attempts"))
                            .successCount(rs.getInt("success_count"))
                            .failureCount(rs.getInt("failure_count"))
                            .averageConfidence(rs.getBigDecimal("avg_confidence"))
                            .fallbackCount(0) // Need specific data for this
                            .build();
                }, params.toArray());

        int totalExtractions = providerStats.stream().mapToInt(ExtractionPerformanceResponse.ProviderStats::getAttempts)
                .sum();
        int totalSuccess = providerStats.stream().mapToInt(ExtractionPerformanceResponse.ProviderStats::getSuccessCount)
                .sum();

        BigDecimal successRate = totalExtractions > 0
                ? BigDecimal.valueOf(totalSuccess).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalExtractions), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Failure Reasons (mocking as we don't have a distinct error log table linked
        // here easily yet,
        // usually would come from audit logs or specific error columns)
        List<ExtractionPerformanceResponse.FailureReason> failureReasons = new ArrayList<>();

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 300) {
            log.warn("Slow extraction performance query: {} ms", duration);
        }

        return ExtractionPerformanceResponse.builder()
                .totalExtractions(totalExtractions)
                .successRate(successRate)
                .averageConfidence(BigDecimal.ZERO) // calculate weighted avg if needed
                .averageDuration(BigDecimal.ZERO)
                .byProvider(providerStats)
                .failureReasons(failureReasons)
                .build();
    }
}

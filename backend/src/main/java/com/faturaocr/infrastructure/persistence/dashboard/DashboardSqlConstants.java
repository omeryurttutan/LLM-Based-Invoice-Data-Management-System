package com.faturaocr.infrastructure.persistence.dashboard;

public final class DashboardSqlConstants {

    private DashboardSqlConstants() {
        // Private constructor to prevent instantiation
    }

    public static final String SUMMARY_SQL = """
            SELECT
                COUNT(*) as total_count,
                COALESCE(SUM(CASE WHEN currency = ? THEN total_amount ELSE 0 END), 0) as total_amount,
                COALESCE(AVG(CASE WHEN currency = ? THEN total_amount END), 0) as average_amount,
                COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_count,
                COALESCE(SUM(CASE WHEN status = 'PENDING' AND currency = ? THEN total_amount ELSE 0 END), 0)
                    as pending_amount,
                COUNT(CASE WHEN status = 'VERIFIED' THEN 1 END) as verified_count,
                COALESCE(SUM(CASE WHEN status = 'VERIFIED' AND currency = ? THEN total_amount ELSE 0 END), 0)
                    as verified_amount,
                COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected_count,
                COUNT(CASE WHEN status = 'PROCESSING' THEN 1 END) as processing_count
            FROM invoices
            %s
            """;

    public static final String SOURCE_SQL = """
            SELECT source_type, COUNT(*) as count
            FROM invoices
            %s
            GROUP BY source_type
            """;

    public static final String CONFIDENCE_SQL = """
            SELECT
                COALESCE(AVG(confidence_score), 0) as avg_score,
                COUNT(CASE WHEN confidence_score >= 90 THEN 1 END) as high_conf,
                COUNT(CASE WHEN confidence_score >= 70 AND confidence_score < 90 THEN 1 END) as medium_conf,
                COUNT(CASE WHEN confidence_score < 70 THEN 1 END) as low_conf
            FROM invoices
            %s AND source_type = 'LLM'
            """;

    public static final String CATEGORY_DISTRIBUTION_SQL = """
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

    public static final String MONTHLY_TREND_SQL = """
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

    public static final String TOP_SUPPLIERS_SQL = """
            SELECT
                supplier_name,
                supplier_tax_number,
                COUNT(*) as invoice_count,
                SUM(total_amount) as total_amount
            FROM invoices
            WHERE company_id = ? AND is_deleted = false AND currency = ?
            """;

    public static final String PENDING_ACTIONS_COUNT_SQL = "SELECT COUNT(*) FROM invoices " +
            "WHERE company_id = ? AND is_deleted = false AND status = 'PENDING'";

    public static final String PENDING_ACTIONS_SQL = """
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

    public static final String STATUS_TIMELINE_CREATED_SQL = "SELECT DATE(created_at) as d, COUNT(*) as c "
            + "FROM invoices "
            + "WHERE company_id = ? AND is_deleted = false AND created_at >= ? "
            + "GROUP BY DATE(created_at)";

    public static final String STATUS_TIMELINE_VERIFIED_SQL = "SELECT DATE(verified_at) as d, COUNT(*) as c "
            + "FROM invoices "
            + "WHERE company_id = ? AND is_deleted = false AND verified_at >= ? "
            + "GROUP BY DATE(verified_at)";

    public static final String STATUS_TIMELINE_REJECTED_SQL = "SELECT DATE(rejected_at) as d, COUNT(*) as c "
            + "FROM invoices "
            + "WHERE company_id = ? AND is_deleted = false AND rejected_at >= ? "
            + "GROUP BY DATE(rejected_at)";

    public static final String EXTRACTION_PERFORMANCE_SQL = """
            SELECT
                llm_provider,
                COUNT(*) as attempts,
                COUNT(CASE WHEN status != 'FAILED' THEN 1 END) as success_count,
                COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failure_count,
                AVG(confidence_score) as avg_confidence
            FROM invoices
            %s
            GROUP BY llm_provider
            """;
}

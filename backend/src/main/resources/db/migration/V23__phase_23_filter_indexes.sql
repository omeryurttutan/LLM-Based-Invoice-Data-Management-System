-- ============================================================================
-- Phase 23: Backend — Advanced Filtering and Search API
-- Description: Indexes for new filter columns to optimize search performance
-- ============================================================================

-- source_type index
CREATE INDEX IF NOT EXISTS idx_invoices_source_type ON invoices(source_type);

-- llm_provider index
CREATE INDEX IF NOT EXISTS idx_invoices_llm_provider ON invoices(llm_provider);

-- currency index
CREATE INDEX IF NOT EXISTS idx_invoices_currency ON invoices(currency);

-- confidence_score index
CREATE INDEX IF NOT EXISTS idx_invoices_confidence_score ON invoices(confidence_score);

-- total_amount index (for range filtering)
CREATE INDEX IF NOT EXISTS idx_invoices_total_amount ON invoices(total_amount);

-- Composite index: company_id + invoice_date (for efficient date range filtering within company)
-- Note: Check if already exists, creating if not
CREATE INDEX IF NOT EXISTS idx_invoices_company_invoice_date ON invoices(company_id, invoice_date);

-- Full-text search support (simple LIKE optimization)
-- For supplier_name, lower-case index for case-insensitive search
CREATE INDEX IF NOT EXISTS idx_invoices_supplier_name_lower ON invoices(LOWER(supplier_name));

-- Composite index for Level 2 duplicate detection (Strong Match)
-- Matches on: company_id + supplier_tax_number + invoice_date + total_amount
CREATE INDEX IF NOT EXISTS idx_invoices_dup_check_level2 
ON invoices(company_id, supplier_tax_number, invoice_date, total_amount) 
WHERE is_deleted = FALSE AND supplier_tax_number IS NOT NULL;

-- Index for Level 3 duplicate detection (Fuzzy Match)
-- Matches on: company_id + invoice_date + total_amount
-- Note: Supplier name matching will be done in memory or via partial index if needed, 
-- but date + amount is usually selective enough to narrow down candidates.
CREATE INDEX IF NOT EXISTS idx_invoices_dup_check_level3 
ON invoices(company_id, invoice_date, total_amount) 
WHERE is_deleted = FALSE;

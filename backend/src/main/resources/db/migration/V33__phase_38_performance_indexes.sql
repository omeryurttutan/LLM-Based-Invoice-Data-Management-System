-- Phase 38: Performance Optimization Indexes

-- 1. Invoices: Filter by status and date (Invoice list with filters)
CREATE INDEX IF NOT EXISTS idx_invoices_company_status_date ON invoices(company_id, status, invoice_date DESC);

-- 2. Invoices: Top suppliers aggregation
CREATE INDEX IF NOT EXISTS idx_invoices_company_supplier ON invoices(company_id, supplier_name);

-- 3. Invoices: Category distribution
CREATE INDEX IF NOT EXISTS idx_invoices_company_category ON invoices(company_id, category_id);

-- 4. Invoices: Recent invoices (Dashboard)
CREATE INDEX IF NOT EXISTS idx_invoices_company_created ON invoices(company_id, created_at DESC);

-- 5. Notifications: Unread count
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, is_read, created_at DESC);

-- 6. Audit Logs: Pagination
CREATE INDEX IF NOT EXISTS idx_audit_logs_company_created ON audit_logs(company_id, created_at DESC);

-- 7. Invoice Versions: History
CREATE INDEX IF NOT EXISTS idx_invoice_versions_invoice_version ON invoice_versions(invoice_id, version_number DESC);

-- 8. Templates: Lookup
CREATE INDEX IF NOT EXISTS idx_templates_company_tax_hash ON supplier_templates(company_id, supplier_tax_number_hash);

-- 9. Rules: Active rules
CREATE INDEX IF NOT EXISTS idx_rules_company_active_priority ON automation_rules(company_id, is_active, priority);

-- 10. User Consents: Lookup
CREATE INDEX IF NOT EXISTS idx_user_consents_user_type_created ON user_consents(user_id, consent_type, granted_at DESC);

-- 11. Partial Indexes for Soft Delete (optimization for active records)
CREATE INDEX IF NOT EXISTS idx_invoices_active_partial ON invoices(company_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_categories_active_partial ON categories(company_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_rules_active_partial ON automation_rules(company_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_templates_active_partial ON supplier_templates(company_id) WHERE is_active = true;

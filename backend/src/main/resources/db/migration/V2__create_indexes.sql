-- ============================================================================
-- FATURA OCR SYSTEM - DATABASE INDEXES
-- Version: 2
-- Description: Creates indexes for query optimization
-- ============================================================================

-- ============================================================================
-- SECTION 1: COMPANIES INDEXES
-- ============================================================================

CREATE INDEX idx_companies_tax_number ON companies(tax_number) WHERE is_deleted = FALSE;
CREATE INDEX idx_companies_is_active ON companies(is_active) WHERE is_deleted = FALSE;

-- ============================================================================
-- SECTION 2: USERS INDEXES
-- ============================================================================

-- Email lookup (login)
CREATE INDEX idx_users_email ON users(email) WHERE is_deleted = FALSE;

-- Company users listing
CREATE INDEX idx_users_company_id ON users(company_id) WHERE is_deleted = FALSE;

-- Role-based queries
CREATE INDEX idx_users_role ON users(role) WHERE is_deleted = FALSE;

-- Active users
CREATE INDEX idx_users_is_active ON users(is_active) WHERE is_deleted = FALSE;

-- Composite: Company + Email (login within company)
CREATE INDEX idx_users_company_email ON users(company_id, email) WHERE is_deleted = FALSE;

-- ============================================================================
-- SECTION 3: CATEGORIES INDEXES
-- ============================================================================

CREATE INDEX idx_categories_company_id ON categories(company_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_categories_parent_id ON categories(parent_id) WHERE is_deleted = FALSE;

-- ============================================================================
-- SECTION 4: INVOICES INDEXES (Most Critical)
-- ============================================================================

-- Company invoices (most common query)
CREATE INDEX idx_invoices_company_id ON invoices(company_id) WHERE is_deleted = FALSE;

-- Status filtering
CREATE INDEX idx_invoices_status ON invoices(status) WHERE is_deleted = FALSE;

-- Date range queries
CREATE INDEX idx_invoices_invoice_date ON invoices(invoice_date) WHERE is_deleted = FALSE;

-- Due date queries (for payment tracking)
CREATE INDEX idx_invoices_due_date ON invoices(due_date) WHERE is_deleted = FALSE;

-- Category filtering
CREATE INDEX idx_invoices_category_id ON invoices(category_id) WHERE is_deleted = FALSE;

-- Source type filtering (LLM vs Manual vs E-Invoice)
CREATE INDEX idx_invoices_source_type ON invoices(source_type) WHERE is_deleted = FALSE;

-- LLM provider tracking
CREATE INDEX idx_invoices_llm_provider ON invoices(llm_provider) WHERE is_deleted = FALSE AND llm_provider IS NOT NULL;

-- Supplier lookup
CREATE INDEX idx_invoices_supplier_name ON invoices(supplier_name) WHERE is_deleted = FALSE;
CREATE INDEX idx_invoices_supplier_tax_number ON invoices(supplier_tax_number) WHERE is_deleted = FALSE;

-- Composite: Company + Invoice Number (uniqueness within company)
CREATE UNIQUE INDEX idx_invoices_company_invoice_number ON invoices(company_id, invoice_number) WHERE is_deleted = FALSE;

-- Composite: Company + Status (common filter)
CREATE INDEX idx_invoices_company_status ON invoices(company_id, status) WHERE is_deleted = FALSE;

-- Composite: Company + Date (date range queries)
CREATE INDEX idx_invoices_company_date ON invoices(company_id, invoice_date DESC) WHERE is_deleted = FALSE;

-- Composite: Status + Date (dashboard queries)
CREATE INDEX idx_invoices_status_date ON invoices(status, invoice_date DESC) WHERE is_deleted = FALSE;

-- Full-text search on supplier name (optional, for search feature)
CREATE INDEX idx_invoices_supplier_name_trgm ON invoices USING gin(supplier_name gin_trgm_ops);

-- Confidence score filtering (for low confidence alerts)
CREATE INDEX idx_invoices_confidence_score ON invoices(confidence_score) WHERE confidence_score IS NOT NULL AND confidence_score < 70;

-- ============================================================================
-- SECTION 5: INVOICE_ITEMS INDEXES
-- ============================================================================

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);

-- ============================================================================
-- SECTION 6: AUDIT_LOGS INDEXES
-- ============================================================================

-- User activity lookup
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);

-- Entity history lookup
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- Action type filtering
CREATE INDEX idx_audit_logs_action_type ON audit_logs(action_type);

-- Time-based queries
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- Composite: Entity + Time (entity history)
CREATE INDEX idx_audit_logs_entity_time ON audit_logs(entity_type, entity_id, created_at DESC);

-- ============================================================================
-- SECTION 7: REFRESH_TOKENS INDEXES
-- ============================================================================

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at) WHERE revoked_at IS NULL;

-- ============================================================================
-- SECTION 8: INVOICE_VERSIONS INDEXES
-- ============================================================================

CREATE INDEX idx_invoice_versions_invoice_id ON invoice_versions(invoice_id);

-- ============================================================================
-- SECTION 9: NOTIFICATIONS INDEXES
-- ============================================================================

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- ============================================================================
-- ENABLE TRIGRAM EXTENSION (for full-text search)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ============================================================================
-- INDEXES COMPLETE
-- ============================================================================

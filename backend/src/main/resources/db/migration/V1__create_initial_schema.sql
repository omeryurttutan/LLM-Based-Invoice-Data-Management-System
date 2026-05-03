-- ============================================================================
-- FATURA OCR SYSTEM - INITIAL DATABASE SCHEMA
-- Version: 1
-- Description: Creates all core tables for the invoice OCR system
-- ============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- SECTION 1: COMPANIES TABLE
-- ============================================================================
-- Companies are the top-level tenant in the multi-tenant architecture.
-- All main entities belong to a company for data isolation.

CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Company Information
    name VARCHAR(255) NOT NULL,
    tax_number VARCHAR(20) UNIQUE,           -- Vergi Numarası (VKN)
    tax_office VARCHAR(255),                  -- Vergi Dairesi
    address TEXT,
    city VARCHAR(100),
    district VARCHAR(100),
    postal_code VARCHAR(10),
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(255),
    
    -- Settings
    default_currency VARCHAR(3) DEFAULT 'TRY',
    invoice_prefix VARCHAR(10),
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Soft Delete
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    
    -- Audit Fields
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE companies IS 'Companies/Organizations using the system (multi-tenant root)';
COMMENT ON COLUMN companies.tax_number IS 'Turkish Tax Number (VKN) - 10 digits for companies';
COMMENT ON COLUMN companies.tax_office IS 'Tax office name (Vergi Dairesi)';

-- ============================================================================
-- SECTION 2: USERS TABLE
-- ============================================================================
-- Users belong to a company and have role-based access control.

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Authentication
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,      -- BCrypt hash
    
    -- Profile
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    
    -- Role & Permissions
    role VARCHAR(20) NOT NULL DEFAULT 'ACCOUNTANT',
    -- Roles: ADMIN, MANAGER, ACCOUNTANT, INTERN
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    
    -- Security
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,
    
    -- Soft Delete
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    
    -- Audit Fields
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT users_email_company_unique UNIQUE (email, company_id),
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN'))
);

COMMENT ON TABLE users IS 'System users with role-based access control';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password (strength 12)';
COMMENT ON COLUMN users.role IS 'User role: ADMIN, MANAGER, ACCOUNTANT, INTERN';
COMMENT ON COLUMN users.failed_login_attempts IS 'Counter for consecutive failed logins - resets on success';
COMMENT ON COLUMN users.locked_until IS 'Account lockout expiry - NULL means not locked';

-- ============================================================================
-- SECTION 3: CATEGORIES TABLE
-- ============================================================================
-- Invoice categories for organization and reporting.

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Category Information
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(7),                         -- Hex color code (#RRGGBB)
    icon VARCHAR(50),                         -- Icon identifier
    
    -- Hierarchy (optional)
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Soft Delete
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    
    -- Audit Fields
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT categories_name_company_unique UNIQUE (name, company_id)
);

COMMENT ON TABLE categories IS 'Invoice categories for organization and reporting';
COMMENT ON COLUMN categories.color IS 'Category color in hex format (#RRGGBB)';
COMMENT ON COLUMN categories.parent_id IS 'Parent category for hierarchical organization';

-- ============================================================================
-- SECTION 4: INVOICES TABLE
-- ============================================================================
-- Main invoice table storing extracted or manually entered invoice data.

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    verified_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    
    -- Invoice Identification
    invoice_number VARCHAR(50) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE,
    
    -- Supplier Information
    supplier_name VARCHAR(255) NOT NULL,
    supplier_tax_number VARCHAR(20),          -- VKN (10) or TCKN (11)
    supplier_tax_office VARCHAR(255),
    supplier_address TEXT,
    supplier_phone VARCHAR(20),
    supplier_email VARCHAR(255),
    
    -- Financial Details
    subtotal DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'TRY',
    exchange_rate DECIMAL(10, 4) DEFAULT 1.0,
    
    -- Status & Workflow
    status VARCHAR(20) DEFAULT 'PENDING',
    -- Status: PENDING, VERIFIED, REJECTED, PROCESSING
    
    -- Source Information
    source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    -- Source: LLM, E_INVOICE, MANUAL
    
    -- LLM Processing Details (when source_type = 'LLM')
    llm_provider VARCHAR(20),
    -- Provider: GEMINI, GPT, CLAUDE
    confidence_score DECIMAL(5, 2),           -- 0.00 to 100.00
    processing_duration_ms INTEGER,           -- Processing time in milliseconds
    
    -- File Information
    original_file_path VARCHAR(500),
    original_file_name VARCHAR(255),
    original_file_size INTEGER,               -- Size in bytes
    original_file_type VARCHAR(50),           -- MIME type
    
    -- E-Invoice Specific (when source_type = 'E_INVOICE')
    e_invoice_uuid VARCHAR(36),               -- GİB UUID
    e_invoice_ettn VARCHAR(36),               -- ETTN
    
    -- Notes
    notes TEXT,
    rejection_reason TEXT,
    
    -- Timestamps
    verified_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    
    -- Soft Delete
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    
    -- Audit Fields
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT invoices_status_check CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED', 'PROCESSING')),
    CONSTRAINT invoices_source_type_check CHECK (source_type IN ('LLM', 'E_INVOICE', 'MANUAL')),
    CONSTRAINT invoices_llm_provider_check CHECK (llm_provider IS NULL OR llm_provider IN ('GEMINI', 'GPT', 'CLAUDE')),
    CONSTRAINT invoices_confidence_score_check CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 100)),
    CONSTRAINT invoices_currency_check CHECK (currency IN ('TRY', 'USD', 'EUR', 'GBP'))
);

COMMENT ON TABLE invoices IS 'Main invoice table storing extracted or manually entered data';
COMMENT ON COLUMN invoices.source_type IS 'How the invoice was created: LLM (extracted), E_INVOICE (parsed XML), MANUAL (user input)';
COMMENT ON COLUMN invoices.llm_provider IS 'Which LLM was used for extraction: GEMINI, GPT, or CLAUDE';
COMMENT ON COLUMN invoices.confidence_score IS 'LLM extraction confidence score (0-100)';
COMMENT ON COLUMN invoices.e_invoice_uuid IS 'GİB e-Invoice UUID for e-invoices';
COMMENT ON COLUMN invoices.e_invoice_ettn IS 'GİB ETTN (Elektronik Takip Numarası)';

-- ============================================================================
-- SECTION 5: INVOICE_ITEMS TABLE
-- ============================================================================
-- Line items for each invoice.

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    
    -- Item Details
    line_number INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(15, 4) NOT NULL DEFAULT 1,
    unit VARCHAR(20) DEFAULT 'ADET',          -- Unit of measure
    unit_price DECIMAL(15, 4) NOT NULL DEFAULT 0,
    
    -- Tax Details
    tax_rate DECIMAL(5, 2) DEFAULT 18.00,     -- KDV rate (%)
    tax_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    
    -- Totals
    subtotal DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    
    -- Optional Details
    product_code VARCHAR(50),
    barcode VARCHAR(50),
    
    -- Audit Fields
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT invoice_items_line_unique UNIQUE (invoice_id, line_number)
);

COMMENT ON TABLE invoice_items IS 'Line items (rows) for each invoice';
COMMENT ON COLUMN invoice_items.unit IS 'Unit of measure: ADET, KG, LT, M, M2, M3, etc.';
COMMENT ON COLUMN invoice_items.tax_rate IS 'KDV (VAT) rate percentage';

-- ============================================================================
-- SECTION 6: AUDIT_LOGS TABLE
-- ============================================================================
-- Immutable audit trail for all data changes.

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Who
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    user_email VARCHAR(255),                  -- Snapshot in case user is deleted
    
    -- What
    action_type VARCHAR(20) NOT NULL,
    -- Action: CREATE, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT
    
    entity_type VARCHAR(50) NOT NULL,         -- Table/entity name
    entity_id UUID,                           -- ID of affected entity
    
    -- Changes (JSONB for flexibility)
    old_value JSONB,                          -- Previous state (for UPDATE/DELETE)
    new_value JSONB,                          -- New state (for CREATE/UPDATE)
    
    -- Context
    ip_address INET,
    user_agent TEXT,
    request_id VARCHAR(36),                   -- For request correlation
    
    -- Additional Info
    description TEXT,
    metadata JSONB,                           -- Any additional context
    
    -- Timestamp (immutable)
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT audit_logs_action_check CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'EXPORT', 'VERIFY', 'REJECT'))
);

-- Make audit_logs immutable (no UPDATE or DELETE)
CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs cannot be modified or deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_modification();

COMMENT ON TABLE audit_logs IS 'Immutable audit trail for all system actions';
COMMENT ON COLUMN audit_logs.old_value IS 'Previous state as JSONB (for UPDATE/DELETE)';
COMMENT ON COLUMN audit_logs.new_value IS 'New state as JSONB (for CREATE/UPDATE)';

-- ============================================================================
-- SECTION 7: REFRESH_TOKENS TABLE
-- ============================================================================
-- JWT refresh tokens for authentication.

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Token
    token_hash VARCHAR(255) NOT NULL UNIQUE,  -- SHA-256 hash of token
    
    -- Metadata
    device_info VARCHAR(255),
    ip_address INET,
    
    -- Validity
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for session management';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of the refresh token';

-- ============================================================================
-- SECTION 8: INVOICE_VERSIONS TABLE (for version history)
-- ============================================================================
-- Stores historical versions of invoices for undo/audit.

CREATE TABLE invoice_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    
    -- Version Info
    version_number INTEGER NOT NULL,
    
    -- Snapshot of invoice data at this version
    invoice_data JSONB NOT NULL,
    
    -- Who made the change
    changed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    change_reason TEXT,
    
    -- Timestamp
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT invoice_versions_unique UNIQUE (invoice_id, version_number)
);

COMMENT ON TABLE invoice_versions IS 'Historical versions of invoices for version history feature';
COMMENT ON COLUMN invoice_versions.invoice_data IS 'Complete invoice snapshot as JSONB';

-- ============================================================================
-- SECTION 9: NOTIFICATION_SETTINGS TABLE
-- ============================================================================
-- User notification preferences.

CREATE TABLE notification_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    
    -- Channel Preferences
    email_enabled BOOLEAN DEFAULT TRUE,
    push_enabled BOOLEAN DEFAULT TRUE,
    in_app_enabled BOOLEAN DEFAULT TRUE,
    
    -- Event Preferences (JSONB for flexibility)
    event_preferences JSONB DEFAULT '{
        "invoice_processed": true,
        "invoice_verified": true,
        "low_confidence": true,
        "bulk_complete": true,
        "system_alert": true
    }'::jsonb,
    
    -- Audit
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE notification_settings IS 'User notification preferences';

-- ============================================================================
-- SECTION 10: NOTIFICATIONS TABLE
-- ============================================================================
-- In-app notifications.

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Notification Content
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    -- Types: INFO, SUCCESS, WARNING, ERROR
    
    -- Related Entity (optional)
    entity_type VARCHAR(50),
    entity_id UUID,
    
    -- Action (optional)
    action_url VARCHAR(500),
    
    -- Status
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT notifications_type_check CHECK (type IN ('INFO', 'SUCCESS', 'WARNING', 'ERROR'))
);

COMMENT ON TABLE notifications IS 'In-app notifications for users';

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================

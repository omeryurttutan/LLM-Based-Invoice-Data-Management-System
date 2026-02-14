# PHASE 3: DATABASE SCHEMA AND MIGRATION INFRASTRUCTURE

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
- **Database**: PostgreSQL 15+
- **Migration Tool**: Flyway 9.x

### Current State
**Phase 0, 1, and 2 have been completed:**
- ✅ Docker Compose environment with PostgreSQL 15
- ✅ CI/CD Pipeline with GitHub Actions
- ✅ Hexagonal Architecture layer structure
- ✅ Base classes (BaseEntity, ValueObject, etc.) created
- ✅ ArchUnit tests enforcing architecture rules

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Design and implement a complete database schema following 3NF (Third Normal Form) normalization using Flyway migrations. The schema must support multi-tenant architecture (company-based isolation), soft delete, audit logging, and be optimized with proper indexes for the expected query patterns.

---

## DATABASE DESIGN PRINCIPLES

### 1. Normalization (3NF)
- **1NF**: All columns contain atomic values
- **2NF**: All non-key attributes depend on the entire primary key
- **3NF**: No transitive dependencies (non-key attributes don't depend on other non-key attributes)

### 2. Multi-Tenancy
- Company-based data isolation
- Every main entity has `company_id` foreign key
- Queries always filter by company

### 3. UUID Primary Keys
- Security (not guessable)
- Distributed system ready
- No business information leaked in URLs

### 4. Soft Delete
- `is_deleted` boolean flag
- `deleted_at` timestamp
- Data preserved for audit/legal requirements

### 5. Audit Fields
- `created_at` timestamp
- `updated_at` timestamp
- Separate `audit_logs` table for change history

---

## DETAILED REQUIREMENTS

### 1. Flyway Configuration

**Purpose**: Set up Flyway for version-controlled database migrations.

#### 1.1 Verify Flyway Dependency (pom.xml)
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

#### 1.2 Application Configuration (application.yml)
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    out-of-order: false
```

#### 1.3 Migration File Naming Convention
```
V{version}__{description}.sql

Examples:
V1__create_initial_schema.sql
V2__add_invoice_items_table.sql
V3__add_indexes.sql
```

---

### 2. Database Schema Overview

```
┌─────────────────┐       ┌─────────────────┐
│   companies     │       │   categories    │
│─────────────────│       │─────────────────│
│ id (PK)         │       │ id (PK)         │
│ name            │       │ name            │
│ tax_number      │       │ description     │
│ ...             │       │ company_id (FK) │
└────────┬────────┘       └────────┬────────┘
         │                         │
         │ 1:N                     │ 1:N
         ▼                         │
┌─────────────────┐                │
│     users       │                │
│─────────────────│                │
│ id (PK)         │                │
│ company_id (FK) │                │
│ email           │                │
│ role            │                │
│ ...             │                │
└─────────────────┘                │
         │                         │
         │ (created_by)            │
         ▼                         ▼
┌─────────────────────────────────────────────┐
│                  invoices                    │
│─────────────────────────────────────────────│
│ id (PK)                                     │
│ company_id (FK)                             │
│ category_id (FK)                            │
│ created_by_user_id (FK)                     │
│ invoice_number, invoice_date, due_date      │
│ supplier_name, supplier_tax_number          │
│ subtotal, tax_amount, total_amount          │
│ currency, status                            │
│ source_type (LLM/E_INVOICE/MANUAL)          │
│ llm_provider (GEMINI/GPT/CLAUDE)            │
│ confidence_score                            │
│ ...                                         │
└─────────────────┬───────────────────────────┘
                  │
                  │ 1:N
                  ▼
┌─────────────────────────────────────────────┐
│              invoice_items                   │
│─────────────────────────────────────────────│
│ id (PK)                                     │
│ invoice_id (FK)                             │
│ description, quantity, unit_price           │
│ tax_rate, tax_amount, total_amount          │
│ ...                                         │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│               audit_logs                     │
│─────────────────────────────────────────────│
│ id (PK)                                     │
│ user_id (FK)                                │
│ action_type (CREATE/UPDATE/DELETE)          │
│ entity_type, entity_id                      │
│ old_value (JSONB), new_value (JSONB)        │
│ ip_address, timestamp                       │
└─────────────────────────────────────────────┘
```

---

### 3. Migration Files

#### 3.1 V1__create_initial_schema.sql

**File**: `backend/src/main/resources/db/migration/V1__create_initial_schema.sql`

```sql
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
```

---

#### 3.2 V2__create_indexes.sql

**File**: `backend/src/main/resources/db/migration/V2__create_indexes.sql`

```sql
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
```

---

#### 3.3 V3__insert_default_data.sql

**File**: `backend/src/main/resources/db/migration/V3__insert_default_data.sql`

```sql
-- ============================================================================
-- FATURA OCR SYSTEM - DEFAULT DATA
-- Version: 3
-- Description: Insert default/seed data for initial setup
-- ============================================================================

-- ============================================================================
-- SECTION 1: DEFAULT COMPANY (for development/testing)
-- ============================================================================

INSERT INTO companies (
    id, 
    name, 
    tax_number, 
    tax_office, 
    default_currency,
    is_active
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Demo Şirketi',
    '1234567890',
    'Test Vergi Dairesi',
    'TRY',
    TRUE
) ON CONFLICT DO NOTHING;

-- ============================================================================
-- SECTION 2: DEFAULT ADMIN USER
-- Password: Admin123! (BCrypt hash with strength 12)
-- ============================================================================

INSERT INTO users (
    id,
    company_id,
    email,
    password_hash,
    full_name,
    role,
    is_active,
    email_verified,
    email_verified_at
) VALUES (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'admin@demo.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.HlGvoXe.BxGX2u',
    'Sistem Yöneticisi',
    'ADMIN',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- ============================================================================
-- SECTION 3: DEFAULT CATEGORIES
-- ============================================================================

INSERT INTO categories (id, company_id, name, description, color, is_active) VALUES
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Genel Giderler', 'Genel işletme giderleri', '#6B7280', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Hammadde', 'Üretim hammaddeleri', '#10B981', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Hizmet Alımları', 'Dış kaynak hizmetleri', '#3B82F6', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Kira', 'Kira ve kira giderleri', '#F59E0B', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Elektrik/Su/Doğalgaz', 'Fatura giderleri', '#EF4444', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Personel', 'Personel giderleri', '#8B5CF6', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Teknoloji', 'Yazılım ve donanım', '#EC4899', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Pazarlama', 'Reklam ve pazarlama', '#14B8A6', TRUE)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- SECTION 4: DEFAULT NOTIFICATION SETTINGS FOR ADMIN
-- ============================================================================

INSERT INTO notification_settings (
    user_id,
    email_enabled,
    push_enabled,
    in_app_enabled
) VALUES (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
    TRUE,
    TRUE,
    TRUE
) ON CONFLICT DO NOTHING;

-- ============================================================================
-- DEFAULT DATA COMPLETE
-- ============================================================================
```

---

### 4. Updated Timestamp Trigger

#### 4.1 V4__create_update_timestamp_trigger.sql

**File**: `backend/src/main/resources/db/migration/V4__create_update_timestamp_trigger.sql`

```sql
-- ============================================================================
-- FATURA OCR SYSTEM - AUTO UPDATE TIMESTAMP TRIGGER
-- Version: 4
-- Description: Creates trigger to auto-update updated_at column
-- ============================================================================

-- Create function to update timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables with updated_at column
CREATE TRIGGER update_companies_updated_at
    BEFORE UPDATE ON companies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_invoice_items_updated_at
    BEFORE UPDATE ON invoice_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notification_settings_updated_at
    BEFORE UPDATE ON notification_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TRIGGERS COMPLETE
-- ============================================================================
```

---

## TESTING REQUIREMENTS

### Test 1: Run Flyway Migration
```bash
cd backend
mvn flyway:migrate

# Or via Spring Boot
mvn spring-boot:run
# Check logs for "Successfully applied X migrations"
```

### Test 2: Verify Tables Created
```bash
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c "
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;
"
```

**Expected Output**:
```
       table_name       
------------------------
 audit_logs
 categories
 companies
 flyway_schema_history
 invoice_items
 invoice_versions
 invoices
 notification_settings
 notifications
 refresh_tokens
 users
(11 rows)
```

### Test 3: Verify Indexes Created
```bash
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c "
SELECT indexname, tablename 
FROM pg_indexes 
WHERE schemaname = 'public' 
AND indexname LIKE 'idx_%'
ORDER BY tablename, indexname;
"
```

### Test 4: Verify Default Data
```bash
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c "
SELECT email, role FROM users;
SELECT name FROM categories;
SELECT name FROM companies;
"
```

### Test 5: Test Audit Log Immutability
```bash
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c "
INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id) 
VALUES ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'CREATE', 'test', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01');

-- This should fail:
UPDATE audit_logs SET action_type = 'DELETE' WHERE entity_type = 'test';
"
# Expected: ERROR: Audit logs cannot be modified or deleted
```

### Test 6: Test Updated_at Trigger
```bash
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c "
SELECT updated_at FROM companies WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
UPDATE companies SET name = 'Demo Şirketi Updated' WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
SELECT updated_at FROM companies WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
"
# updated_at should be different
```

---

## VERIFICATION CHECKLIST

After completing this phase, verify all items:

- [ ] Flyway dependencies added to pom.xml
- [ ] Flyway configuration in application.yml
- [ ] V1__create_initial_schema.sql created with all tables
- [ ] V2__create_indexes.sql created with all indexes
- [ ] V3__insert_default_data.sql created with seed data
- [ ] V4__create_update_timestamp_trigger.sql created
- [ ] All migrations run successfully
- [ ] 10 tables created (excluding flyway_schema_history)
- [ ] companies table has all required columns
- [ ] users table has all required columns including security fields
- [ ] invoices table has source_type, llm_provider, confidence_score
- [ ] invoice_items table created with FK to invoices
- [ ] categories table created with company isolation
- [ ] audit_logs table is immutable (trigger working)
- [ ] refresh_tokens table created for JWT
- [ ] All indexes created and verified
- [ ] Default company and admin user created
- [ ] Default categories created
- [ ] updated_at trigger working on all tables
- [ ] pg_trgm extension enabled for full-text search
- [ ] Soft delete columns (is_deleted, deleted_at) on all main tables

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_3_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed
- Actual time spent vs estimated (2-3 days)

### 2. Completed Tasks
List each task with checkbox.

### 3. Migration Files Created
```
backend/src/main/resources/db/migration/
├── V1__create_initial_schema.sql
├── V2__create_indexes.sql
├── V3__insert_default_data.sql
└── V4__create_update_timestamp_trigger.sql
```

### 4. Tables Created
List all tables with their column counts.

### 5. Indexes Created
List all indexes grouped by table.

### 6. Flyway Migration Output
```bash
$ mvn flyway:migrate
# Include actual output
```

### 7. Verification Query Results
Include actual output from all test queries.

### 8. ER Diagram
Include or link to Entity-Relationship diagram.

### 9. Issues Encountered
Document any problems and solutions.

### 10. Next Steps
What needs to be done in Phase 4 (Authentication).

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 0**: Development Environment Setup ✅
- **Phase 1**: CI/CD Pipeline Setup ✅
- **Phase 2**: Hexagonal Architecture ✅

### Required By (blocks these phases)
- **Phase 4**: Authentication (needs users table)
- **Phase 5**: RBAC (needs users.role)
- **Phase 7**: Invoice CRUD API (needs invoices table)
- **Phase 8**: Audit Log (needs audit_logs table)
- All subsequent phases that interact with database

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ All 4 migration files created and valid
2. ✅ Flyway migrations run without errors
3. ✅ All 10 tables created with correct structure
4. ✅ All indexes created (30+ indexes)
5. ✅ Default company and admin user exist
6. ✅ Default categories created
7. ✅ Audit log immutability trigger works
8. ✅ Updated_at auto-update trigger works
9. ✅ Soft delete columns on all main tables
10. ✅ Result file is created with complete documentation

---

## IMPORTANT NOTES

1. **Migration Order**: Migrations must run in version order (V1, V2, V3, V4)
2. **Idempotent Seed Data**: Use `ON CONFLICT DO NOTHING` for seed data
3. **Password Hash**: Default admin password is "Admin123!" - change in production
4. **UUID Extension**: Must be enabled before creating tables
5. **Index Strategy**: Partial indexes (WHERE is_deleted = FALSE) for efficiency
6. **JSONB**: Used for flexible data (old_value, new_value, event_preferences)

---

## ROLLBACK PLAN

If rollback is needed, use Flyway's built-in mechanism:

```bash
# Show migration status
mvn flyway:info

# Clean database (DEVELOPMENT ONLY - destroys all data)
mvn flyway:clean

# Or manually:
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c "
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO fatura_user;
"
```

**WARNING**: Never use `flyway:clean` in production!

---

**Phase 3 Completion Target**: Complete, normalized, indexed database ready for application development

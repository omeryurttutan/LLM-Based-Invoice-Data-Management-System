-- ============================================================================
-- FATURA OCR SYSTEM - MULTI-TENANT USER ACCESS
-- Version: 44
-- Description: Creates user_company_access table to support many-to-many 
--              relationship between users and companies for accountants.
-- ============================================================================

CREATE TABLE user_company_access (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Role specifically for this company
    role VARCHAR(20) NOT NULL DEFAULT 'ACCOUNTANT',
    
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Audit Fields
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT user_company_access_unique UNIQUE (user_id, company_id),
    CONSTRAINT user_company_access_role_check CHECK (role IN ('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN'))
);

-- 1. Migrate existing user-company relationships to the new junction table
INSERT INTO user_company_access (user_id, company_id, role)
SELECT id, company_id, role FROM users WHERE company_id IS NOT NULL;

-- 2. Modify users table to rename company_id to default_company_id
-- We must first drop constraints that depend on company_id
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_company_unique;
ALTER TABLE users ADD CONSTRAINT users_email_unique UNIQUE (email);

ALTER TABLE users RENAME COLUMN company_id TO default_company_id;
ALTER TABLE users ALTER COLUMN default_company_id DROP NOT NULL;

-- Keep the role column on users for "system-level" role (like Super Admin),
-- or default role for their primary company.

-- Update comments
COMMENT ON TABLE user_company_access IS 'Many-to-many mapping for users and companies (Multi-Tenant)';
COMMENT ON COLUMN users.default_company_id IS 'The default or most recently active company for this user';

-- ============================================================================
-- FATURA OCR SYSTEM - SUPER ADMIN & SaaS SUBSCRIPTION SUPPORT
-- Version: 46
-- Description: Adds SUPER_ADMIN role support, subscription/trial fields,
--              and quota management for SaaS business model.
-- ============================================================================

-- ============================================================================
-- SECTION 1: UPDATE USERS ROLE CONSTRAINT FOR SUPER_ADMIN
-- ============================================================================

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check 
    CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN'));

-- Note: company_id was already renamed to default_company_id in V44
-- and NOT NULL was already dropped there, so no action needed here.

-- ============================================================================
-- SECTION 2: SUBSCRIPTION & QUOTA FIELDS ON COMPANIES
-- ============================================================================

-- Subscription status: TRIAL, ACTIVE, SUSPENDED, CANCELLED
ALTER TABLE companies ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(20) DEFAULT 'TRIAL';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMPTZ;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS plan_id VARCHAR(50) DEFAULT 'FREE_TRIAL';

-- Quota limits
ALTER TABLE companies ADD COLUMN IF NOT EXISTS max_users INT DEFAULT 2;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS max_invoices INT DEFAULT 350;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS daily_invoice_limit INT DEFAULT 50;

-- Usage tracking
ALTER TABLE companies ADD COLUMN IF NOT EXISTS used_invoice_count INT DEFAULT 0;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS daily_invoice_count INT DEFAULT 0;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS daily_count_date DATE DEFAULT CURRENT_DATE;

-- Suspension info
ALTER TABLE companies ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS suspension_reason VARCHAR(500);

-- ============================================================================
-- SECTION 3: UPDATE EXISTING COMPANY (Demo) TO ACTIVE
-- ============================================================================

UPDATE companies 
SET subscription_status = 'ACTIVE', 
    max_users = 100, 
    max_invoices = 999999,
    daily_invoice_limit = 999999,
    plan_id = 'UNLIMITED'
WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================

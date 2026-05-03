-- ============================================================================
-- PHASE 20: FILE UPLOAD INFRASTRUCTURE
-- Version: 8
-- Description: Adds batch_jobs table and file upload columns to invoices
-- ============================================================================

-- 1. Create batch_jobs table
CREATE TABLE batch_jobs (
    id BIGSERIAL PRIMARY KEY,
    batch_id UUID NOT NULL UNIQUE,
    
    -- Ownership
    user_id UUID NOT NULL REFERENCES users(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    -- Stats
    total_files INTEGER NOT NULL DEFAULT 0,
    completed_files INTEGER NOT NULL DEFAULT 0,
    failed_files INTEGER NOT NULL DEFAULT 0,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    -- Status: IN_PROGRESS, COMPLETED, PARTIALLY_COMPLETED, FAILED
    
    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at TIMESTAMPTZ,
    
    CONSTRAINT batch_jobs_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'PARTIALLY_COMPLETED', 'FAILED'))
);

CREATE INDEX idx_batch_jobs_batch_id ON batch_jobs(batch_id);
CREATE INDEX idx_batch_jobs_company_id ON batch_jobs(company_id);

COMMENT ON TABLE batch_jobs IS 'Tracks status of bulk invoice uploads';

-- 2. Add columns to invoices table
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS batch_id UUID REFERENCES batch_jobs(batch_id),
    ADD COLUMN IF NOT EXISTS stored_file_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS file_hash VARCHAR(64);

-- Add index on batch_id in invoices
CREATE INDEX idx_invoices_batch_id ON invoices(batch_id);
CREATE INDEX idx_invoices_file_hash ON invoices(file_hash);

COMMENT ON COLUMN invoices.batch_id IS 'Reference to the bulk upload batch';
COMMENT ON COLUMN invoices.stored_file_path IS 'Full path on the shared volume';
COMMENT ON COLUMN invoices.file_hash IS 'SHA-256 hash for deduplication';

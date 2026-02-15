-- ============================================================================
-- FATURA OCR SYSTEM - PHASE 27: NOTIFICATION SYSTEM
-- Version: 27
-- Description: Creates notifications table and related indexes
-- ============================================================================

-- Drop existing notifications table if it exists (refactoring from V1)
DROP TABLE IF EXISTS notifications CASCADE;

-- Create Notifications Table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Notification Details
    type VARCHAR(50) NOT NULL,
    -- Types: EXTRACTION_COMPLETED, EXTRACTION_FAILED, BATCH_COMPLETED, etc.
    
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    -- Severity: INFO, SUCCESS, WARNING, ERROR
    
    -- Status
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    read_at TIMESTAMPTZ,
    
    -- Reference / Link
    reference_type VARCHAR(50),  -- Renamed from entity_type for clarity (INVOICE, BATCH, SYSTEM)
    reference_id UUID,           -- Renamed from entity_id (UUID type)
    
    -- Extra Data
    metadata JSONB,              -- For flexibility (confidence_score, provider, error details)
    
    -- Audit
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Comments
COMMENT ON TABLE notifications IS 'User notifications for system events';
COMMENT ON COLUMN notifications.type IS 'Event type identifier (e.g. EXTRACTION_COMPLETED)';
COMMENT ON COLUMN notifications.severity IS 'Visual severity level: INFO, SUCCESS, WARNING, ERROR';
COMMENT ON COLUMN notifications.reference_type IS 'Type of the related entity (INVOICE, BATCH, SYSTEM)';

-- Indexes for Performance
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_company_id ON notifications(company_id);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);

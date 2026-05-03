-- ============================================================================
-- FATURA OCR SYSTEM - PHASE 42: FIX NOTIFICATIONS REMAINDER
-- Version: 42
-- Description: Adds missing auditing fields (updated_at, is_deleted, deleted_at)
-- to notifications table to align with BaseJpaEntity requirements.
-- ============================================================================

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

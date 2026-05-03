-- Add extraction_corrections column to invoices table
-- This column stores the corrections made during verification (Phase 22)
-- Used by Template Learning Service (Phase 30-A) to calculate field accuracy

ALTER TABLE invoices
ADD COLUMN extraction_corrections JSONB DEFAULT NULL;

COMMENT ON COLUMN invoices.extraction_corrections IS 'JSON array of corrections made during verification: [{field, original_value, corrected_value}]';

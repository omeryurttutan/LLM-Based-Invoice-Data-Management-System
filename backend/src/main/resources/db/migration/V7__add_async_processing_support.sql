-- Add new statuses to the check constraint
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_status_check;

ALTER TABLE invoices 
ADD CONSTRAINT invoices_status_check 
CHECK (status IN ('PENDING', 'PROCESSING', 'VERIFIED', 'REJECTED', 'QUEUED', 'FAILED'));

-- Add correlation_id column for async request tracking
ALTER TABLE invoices
ADD COLUMN correlation_id VARCHAR(36);

-- Add index on correlation_id for faster lookups
CREATE INDEX idx_invoices_correlation_id ON invoices(correlation_id);

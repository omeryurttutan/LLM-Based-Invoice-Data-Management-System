-- Increase column sizes for encrypted fields (Base64 encoded encrypted data is much longer)
ALTER TABLE users ALTER COLUMN phone TYPE VARCHAR(500);

ALTER TABLE companies ALTER COLUMN tax_number TYPE VARCHAR(500);
ALTER TABLE companies ALTER COLUMN address TYPE VARCHAR(1000); -- Address can be long, encrypted is longer
ALTER TABLE companies ALTER COLUMN phone TYPE VARCHAR(500);

ALTER TABLE invoices ALTER COLUMN supplier_tax_number TYPE VARCHAR(500);

-- Add hash columns for searchable fields
ALTER TABLE companies ADD COLUMN tax_number_hash VARCHAR(64);
ALTER TABLE invoices ADD COLUMN supplier_tax_number_hash VARCHAR(64);

-- Add indexes for hash columns
CREATE INDEX idx_companies_tax_number_hash ON companies(tax_number_hash);
CREATE INDEX idx_invoices_supplier_tax_number_hash ON invoices(supplier_tax_number_hash);

-- Create table for KVKK settings/metadata
CREATE TABLE IF NOT EXISTS system_settings (
    key VARCHAR(100) PRIMARY KEY,
    value VARCHAR(255),
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_settings (key, value, description)
VALUES ('data_encrypted', 'false', 'Flag indicating if existing plain-text data has been migrated to encrypted form');

-- Create user_consents table
CREATE TABLE user_consents (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    consent_type VARCHAR(50) NOT NULL,
    consent_version VARCHAR(20) NOT NULL,
    is_granted BOOLEAN NOT NULL,
    ip_address INET NOT NULL,
    user_agent TEXT,
    granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP,
    metadata JSONB,
    CONSTRAINT uk_user_consent_version UNIQUE (user_id, consent_type, consent_version)
);

CREATE INDEX idx_user_consents_user ON user_consents(user_id);
CREATE INDEX idx_user_consents_company ON user_consents(company_id);
CREATE INDEX idx_user_consents_type ON user_consents(user_id, consent_type, granted_at DESC);

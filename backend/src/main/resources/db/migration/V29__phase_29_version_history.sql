-- Drop existing table if it exists (from V1 or previous attempts)
DROP TABLE IF EXISTS invoice_versions;

CREATE TABLE invoice_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    snapshot_data JSONB NOT NULL,
    items_snapshot JSONB NOT NULL,
    change_source VARCHAR(50) NOT NULL,
    change_summary TEXT,
    changed_fields JSONB,
    changed_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    company_id UUID REFERENCES companies(id),
    UNIQUE(invoice_id, version_number)
);

CREATE INDEX idx_invoice_versions_invoice ON invoice_versions(invoice_id);
CREATE INDEX idx_invoice_versions_invoice_version ON invoice_versions(invoice_id, version_number DESC);
CREATE INDEX idx_invoice_versions_company ON invoice_versions(company_id);

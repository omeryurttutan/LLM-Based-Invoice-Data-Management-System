CREATE TABLE llm_api_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL, -- FK to companies not strictly enforced to allow decoupling
    provider VARCHAR(20) NOT NULL,
    model VARCHAR(50) NOT NULL,
    request_type VARCHAR(30) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    estimated_cost_usd DECIMAL(10,6),
    success BOOLEAN NOT NULL DEFAULT true,
    duration_ms INTEGER,
    invoice_id UUID,
    correlation_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_llm_usage_company_date ON llm_api_usage (company_id, created_at);
CREATE INDEX idx_llm_usage_provider ON llm_api_usage (provider, created_at);

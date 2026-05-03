-- Create supplier_templates table
CREATE TABLE supplier_templates (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    supplier_tax_number VARCHAR(20) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    sample_count INTEGER NOT NULL DEFAULT 0,
    learned_data JSONB NOT NULL DEFAULT '{}',
    default_category_id UUID REFERENCES categories(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_invoice_date TIMESTAMP,
    
    CONSTRAINT uq_supplier_templates_company_tax UNIQUE (company_id, supplier_tax_number)
);

CREATE INDEX idx_supplier_templates_company ON supplier_templates(company_id);
CREATE INDEX idx_supplier_templates_supplier ON supplier_templates(supplier_tax_number);
CREATE INDEX idx_supplier_templates_active ON supplier_templates(company_id, is_active) WHERE is_active = TRUE;

-- Create automation_rules table
CREATE TABLE automation_rules (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    conditions JSONB NOT NULL,
    actions JSONB NOT NULL,
    condition_logic VARCHAR(10) NOT NULL DEFAULT 'AND', -- AND, OR
    priority INTEGER NOT NULL DEFAULT 100,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    trigger_point VARCHAR(50) NOT NULL, -- AFTER_EXTRACTION, AFTER_VERIFICATION, ON_MANUAL_CREATE
    execution_count INTEGER NOT NULL DEFAULT 0,
    last_executed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_user_id UUID REFERENCES users(id)
);

CREATE INDEX idx_automation_rules_company ON automation_rules(company_id);
CREATE INDEX idx_automation_rules_active ON automation_rules(company_id, is_active, trigger_point);

-- Create rule_execution_log table
CREATE TABLE rule_execution_log (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES automation_rules(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id),
    trigger_point VARCHAR(50) NOT NULL,
    conditions_matched JSONB NOT NULL,
    actions_applied JSONB NOT NULL,
    execution_result VARCHAR(20) NOT NULL, -- SUCCESS, FAILED
    error_message TEXT,
    executed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rule_exec_log_invoice ON rule_execution_log(invoice_id);
CREATE INDEX idx_rule_exec_log_rule ON rule_execution_log(rule_id);
CREATE INDEX idx_rule_exec_log_company ON rule_execution_log(company_id, executed_at DESC);

-- ============================================================================
-- FATURA OCR SYSTEM - DASHBOARD SEED DATA
-- Description: Inserts sample invoices and LLM usage for the Demo Company
-- ============================================================================

-- Variables (using Demo Company from V3)
-- Company: a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
-- Admin User: b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22

-- 1. Insert Invoices for Last 6 Months
-- Month 1 (current)
INSERT INTO invoices (id, company_id, category_id, created_by_user_id, invoice_number, invoice_date, supplier_name, subtotal, tax_amount, total_amount, status, source_type, created_at) VALUES
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'INV-2026-001', CURRENT_DATE - INTERVAL '2 days', 'Amazon AWS', 1200.00, 216.00, 1416.00, 'VERIFIED', 'LLM', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'FAT-2026-442', CURRENT_DATE - INTERVAL '5 days', 'EnerjiSa', 850.00, 153.00, 1003.00, 'VERIFIED', 'E_INVOICE', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'INV-2026-002', CURRENT_DATE - INTERVAL '1 days', 'Kırtasiye Dünyası', 150.00, 27.00, 177.00, 'PENDING', 'LLM', CURRENT_TIMESTAMP - INTERVAL '1 days'),
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'HAM-2026-001', CURRENT_DATE - INTERVAL '8 days', 'Maden İş Ltd.', 15000.00, 2700.00, 17700.00, 'VERIFIED', 'MANUAL', CURRENT_TIMESTAMP - INTERVAL '8 days');

-- Month -1
INSERT INTO invoices (id, company_id, category_id, created_by_user_id, invoice_number, invoice_date, supplier_name, subtotal, tax_amount, total_amount, status, source_type, created_at) VALUES
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'KIRA-2026-01', CURRENT_DATE - INTERVAL '1 month', 'Emlak Yönetim', 5000.00, 0.00, 5000.00, 'VERIFIED', 'MANUAL', CURRENT_TIMESTAMP - INTERVAL '1 month'),
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'SRV-2026-99', CURRENT_DATE - INTERVAL '1 month - 5 days', 'Yazılım Çözümleri', 3000.00, 540.00, 3540.00, 'VERIFIED', 'LLM', CURRENT_TIMESTAMP - INTERVAL '1 month - 5 days');

-- Month -2
INSERT INTO invoices (id, company_id, category_id, created_by_user_id, invoice_number, invoice_date, supplier_name, subtotal, tax_amount, total_amount, status, source_type, created_at) VALUES
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'HAM-2025-122', CURRENT_DATE - INTERVAL '2 month', 'Maden İş Ltd.', 12000.00, 2160.00, 14160.00, 'VERIFIED', 'MANUAL', CURRENT_TIMESTAMP - INTERVAL '2 month'),
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'TECH-2025-01', CURRENT_DATE - INTERVAL '2 month - 10 days', 'Apple Türkiye', 45000.00, 8100.00, 53100.00, 'VERIFIED', 'E_INVOICE', CURRENT_TIMESTAMP - INTERVAL '2 month - 10 days');

-- Month -3
INSERT INTO invoices (id, company_id, category_id, created_by_user_id, invoice_number, invoice_date, supplier_name, subtotal, tax_amount, total_amount, status, source_type, created_at) VALUES
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'ADV-2025-55', CURRENT_DATE - INTERVAL '3 month', 'Google Ireland', 25000.00, 0.00, 25000.00, 'VERIFIED', 'LLM', CURRENT_TIMESTAMP - INTERVAL '3 month');

-- Month -4
INSERT INTO invoices (id, company_id, category_id, created_by_user_id, invoice_number, invoice_date, supplier_name, subtotal, tax_amount, total_amount, status, source_type, created_at) VALUES
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'HAM-2025-099', CURRENT_DATE - INTERVAL '4 month', 'Maden İş Ltd.', 18000.00, 3240.00, 21240.00, 'VERIFIED', 'LLM', CURRENT_TIMESTAMP - INTERVAL '4 month');

-- Month -5
INSERT INTO invoices (id, company_id, category_id, created_by_user_id, invoice_number, invoice_date, supplier_name, subtotal, tax_amount, total_amount, status, source_type, created_at) VALUES
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'KIRA-2025-05', CURRENT_DATE - INTERVAL '5 month', 'Emlak Yönetim', 5000.00, 0.00, 5000.00, 'VERIFIED', 'MANUAL', CURRENT_TIMESTAMP - INTERVAL '5 month');

-- 2. Insert LLM API Usage for Performance Chart
INSERT INTO llm_api_usage (company_id, provider, model, request_type, input_tokens, output_tokens, estimated_cost_usd, success, duration_ms, created_at) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'GEMINI', 'gemini-1.5-pro', 'INVOICE_EXTRACTION', 450, 1200, 0.015, true, 2450, CURRENT_TIMESTAMP - INTERVAL '1 days'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'GPT', 'gpt-4o', 'INVOICE_EXTRACTION', 600, 800, 0.042, true, 3100, CURRENT_TIMESTAMP - INTERVAL '2 days'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'GEMINI', 'gemini-1.5-flash', 'INVOICE_EXTRACTION', 420, 1100, 0.002, true, 1100, CURRENT_TIMESTAMP - INTERVAL '3 days'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'CLAUDE', 'claude-3-sonnet', 'INVOICE_EXTRACTION', 550, 950, 0.028, false, 4200, CURRENT_TIMESTAMP - INTERVAL '4 days'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'GEMINI', 'gemini-1.5-pro', 'INVOICE_EXTRACTION', 460, 1250, 0.016, true, 2550, CURRENT_TIMESTAMP - INTERVAL '5 days'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'GPT', 'gpt-4o', 'INVOICE_EXTRACTION', 610, 820, 0.043, true, 3200, CURRENT_TIMESTAMP - INTERVAL '6 days');

-- 3. Additional Pending Invoices for "Bekleyen İşlemler"
INSERT INTO invoices (id, company_id, category_id, created_by_user_id, invoice_number, invoice_date, supplier_name, subtotal, tax_amount, total_amount, status, source_type, created_at) VALUES
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'INV-PEND-001', CURRENT_DATE - INTERVAL '2 hours', 'Shell Market', 450.00, 81.00, 531.00, 'PENDING', 'LLM', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
(uuid_generate_v4(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'INV-PEND-002', CURRENT_DATE - INTERVAL '1 days', 'Temizlik Hizmetleri', 2200.00, 396.00, 2596.00, 'PENDING', 'LLM', CURRENT_TIMESTAMP - INTERVAL '1 days');

-- 4. Audit Logs for some actions
INSERT INTO audit_logs (user_id, user_email, action_type, entity_type, entity_id, description, created_at) VALUES
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'admin@demo.com', 'LOGIN', 'USER', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Sisteme giriş yapıldı', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'admin@demo.com', 'CREATE', 'INVOICE', uuid_generate_v4(), 'Yeni fatura yüklendi: INV-2026-001', CURRENT_TIMESTAMP - INTERVAL '2 days');

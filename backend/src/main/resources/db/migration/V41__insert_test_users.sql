-- ============================================================================
-- FATURA OCR SYSTEM - TEST USERS
-- Version: 41
-- Description: Insert default test users for all roles
-- ============================================================================

-- Use the existing company from V3: a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
-- Password for all below is: Admin123!

INSERT INTO users (
    id,
    company_id,
    email,
    password_hash,
    full_name,
    role,
    is_active,
    email_verified,
    email_verified_at
) VALUES 
(
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'manager@demo.com',
    '$2a$12$xIutmiIFIgdxR7CCYpels.RP1TIf6Rrqzb2ebWZIM0.9ASey4kTZm',
    'Test Yöneticisi',
    'MANAGER',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
),
(
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'accountant@demo.com',
    '$2a$12$xIutmiIFIgdxR7CCYpels.RP1TIf6Rrqzb2ebWZIM0.9ASey4kTZm',
    'Test Muhasebeci',
    'ACCOUNTANT',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
),
(
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'intern@demo.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.HlGvoXe.BxGX2u',
    'Test Stajyer',
    'INTERN',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

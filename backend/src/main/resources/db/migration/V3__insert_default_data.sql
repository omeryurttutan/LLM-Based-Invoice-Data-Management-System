-- ============================================================================
-- FATURA OCR SYSTEM - DEFAULT DATA
-- Version: 3
-- Description: Insert default/seed data for initial setup
-- ============================================================================

-- ============================================================================
-- SECTION 1: DEFAULT COMPANY (for development/testing)
-- ============================================================================

INSERT INTO companies (
    id, 
    name, 
    tax_number, 
    tax_office, 
    default_currency,
    is_active
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Demo Şirketi',
    '1234567890',
    'Test Vergi Dairesi',
    'TRY',
    TRUE
) ON CONFLICT DO NOTHING;

-- ============================================================================
-- SECTION 2: DEFAULT ADMIN USER
-- Password: Admin123! (BCrypt hash with strength 12)
-- ============================================================================

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
) VALUES (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'admin@demo.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.HlGvoXe.BxGX2u',
    'Sistem Yöneticisi',
    'ADMIN',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- ============================================================================
-- SECTION 3: DEFAULT CATEGORIES
-- ============================================================================

INSERT INTO categories (id, company_id, name, description, color, is_active) VALUES
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Genel Giderler', 'Genel işletme giderleri', '#6B7280', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Hammadde', 'Üretim hammaddeleri', '#10B981', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Hizmet Alımları', 'Dış kaynak hizmetleri', '#3B82F6', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Kira', 'Kira ve kira giderleri', '#F59E0B', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Elektrik/Su/Doğalgaz', 'Fatura giderleri', '#EF4444', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Personel', 'Personel giderleri', '#8B5CF6', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Teknoloji', 'Yazılım ve donanım', '#EC4899', TRUE),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Pazarlama', 'Reklam ve pazarlama', '#14B8A6', TRUE)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- SECTION 4: DEFAULT NOTIFICATION SETTINGS FOR ADMIN
-- ============================================================================

INSERT INTO notification_settings (
    user_id,
    email_enabled,
    push_enabled,
    in_app_enabled
) VALUES (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
    TRUE,
    TRUE,
    TRUE
) ON CONFLICT DO NOTHING;

-- ============================================================================
-- DEFAULT DATA COMPLETE
-- ============================================================================

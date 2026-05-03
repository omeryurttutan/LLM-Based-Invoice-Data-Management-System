-- Description: Add missing audit columns to user_company_access to satisfy BaseJpaEntity constraints
ALTER TABLE user_company_access 
ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN deleted_at TIMESTAMPTZ;

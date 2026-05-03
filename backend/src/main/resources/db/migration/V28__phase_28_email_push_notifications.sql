-- Phase 28: Email and Push Notification Support

-- 1. Push Subscriptions Table
CREATE TABLE push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    endpoint TEXT NOT NULL,
    p256dh_key TEXT NOT NULL,
    auth_key TEXT NOT NULL,
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP
);

CREATE INDEX idx_push_subscriptions_user ON push_subscriptions(user_id);

-- 2. Notification Preferences Table
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    preferences JSONB NOT NULL DEFAULT '{}',
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Note: user_id is unique, so we don't strictly need a separate index, 
-- but Postgres creates a unique index on UNIQUE constraints anyway.

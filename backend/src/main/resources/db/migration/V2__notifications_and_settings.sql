-- Flyway Migration V2: Notifications, Organization Settings, and Integrations

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    link_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS organization_settings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE UNIQUE,
    approval_threshold_amount NUMERIC(15, 2) NOT NULL DEFAULT 5000.00,
    matching_tolerance_amount NUMERIC(15, 2) NOT NULL DEFAULT 1.00,
    duplicate_window_days INT NOT NULL DEFAULT 90,
    auto_export_approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS integration_configs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    integration_type VARCHAR(50) NOT NULL,
    endpoint_url VARCHAR(255),
    auth_type VARCHAR(50) NOT NULL DEFAULT 'API_KEY',
    api_key_masked VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_tested_at TIMESTAMP WITH TIME ZONE,
    last_test_result VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_org_user ON notifications(organization_id, user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_integrations_org ON integration_configs(organization_id);

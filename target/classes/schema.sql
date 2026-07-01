-- Enable cryptographic extensions for UUID/Hashing operations
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- System Roles Lookup Table
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

INSERT INTO roles (name)
SELECT v.name FROM (VALUES ('ROLE_ADMIN'), ('ROLE_AUDITOR'), ('ROLE_INTERN')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE roles.name = v.name);

-- Users Registration Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    tin_number VARCHAR(20) UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User-Roles Junction Table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Cryptographically Chained Immutable Audit Log Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL,
    target_table VARCHAR(50) NOT NULL,
    record_id VARCHAR(100) NOT NULL DEFAULT 'N/A',
    ip_address VARCHAR(45) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    exchange_rate NUMERIC(12,4) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    previous_row_hash VARCHAR(64),
    current_row_hash VARCHAR(64) NOT NULL
);

-- Transaction Outbox Table for Load-Shedding / Intermittent Connection Resilience
CREATE TABLE IF NOT EXISTS fiscal_outbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes optimized for fast auditing searches
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_outbox_status_retry ON fiscal_outbox(status, next_attempt_at);

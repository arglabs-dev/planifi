CREATE TABLE IF NOT EXISTS expenses (
    id UUID PRIMARY KEY,
    amount NUMERIC(14, 2) NOT NULL,
    occurred_on DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    request_hash VARCHAR(255) NOT NULL,
    response_body TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_idempotency_key_status
    ON idempotency_keys (idempotency_key, status);

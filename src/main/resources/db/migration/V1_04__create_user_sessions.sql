CREATE TABLE user_sessions
(
    id                 UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    user_id            UUID           NOT NULL,
    session_id         UUID UNIQUE    NOT NULL DEFAULT uuid_generate_v4(),
    refresh_token_hash TEXT           NOT NULL,
    user_agent_enc     BYTEA          NOT NULL,
    user_agent_hash    TEXT           NOT NULL,
    ip_address_enc     BYTEA          NOT NULL,
    ip_address_hash    TEXT           NOT NULL,
    country_code       VARCHAR(10),
    expires_at         TIMESTAMPTZ(6) NOT NULL,
    revoked_at         TIMESTAMPTZ(6),
    deleted_at         TIMESTAMPTZ(6),
    created_at         TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ(6)          DEFAULT now(),
    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions (user_id);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions (expires_at);
CREATE INDEX idx_user_sessions_revoked_at ON user_sessions (revoked_at);
CREATE INDEX idx_user_sessions_deleted_at ON user_sessions (deleted_at);
CREATE INDEX idx_user_sessions_not_deleted ON user_sessions (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_sessions_user_agent_hash ON user_sessions (user_agent_hash);
CREATE INDEX idx_user_sessions_ip_address_hash ON user_sessions (ip_address_hash);
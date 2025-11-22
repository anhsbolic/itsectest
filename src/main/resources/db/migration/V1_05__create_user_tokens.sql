CREATE TABLE user_tokens
(
    id         UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    user_id    UUID           NOT NULL,
    token_hash TEXT           NOT NULL,
    token_type VARCHAR(100)   NOT NULL,
    expires_at TIMESTAMPTZ(6) NOT NULL,
    used_at    TIMESTAMPTZ(6),
    revoked_at TIMESTAMPTZ(6),
    deleted_at TIMESTAMPTZ(6),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ(6)          DEFAULT now(),
    CONSTRAINT fk_user_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX uq_user_tokens_token_hash ON user_tokens (token_hash);

CREATE INDEX idx_user_tokens_user_id ON user_tokens (user_id);
CREATE INDEX idx_user_tokens_token_type ON user_tokens (token_type);
CREATE INDEX idx_user_tokens_expires_at ON user_tokens (expires_at);
CREATE INDEX idx_user_tokens_used_at ON user_tokens (used_at);
CREATE INDEX idx_user_tokens_revoked_at ON user_tokens (revoked_at);
CREATE INDEX idx_user_tokens_deleted_at ON user_tokens (deleted_at);
CREATE INDEX idx_user_tokens_not_deleted ON user_tokens (deleted_at) WHERE deleted_at IS NULL;

-- Optional: prevent duplicated active token per user & type
CREATE UNIQUE INDEX uq_user_tokens_user_type_hash
    ON user_tokens (user_id, token_type, token_hash)
    WHERE deleted_at IS NULL;
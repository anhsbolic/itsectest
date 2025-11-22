CREATE TABLE audit_logs
(
    id              UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    user_id         UUID,
    activity        VARCHAR(255)   NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       UUID,
    success         BOOLEAN        NOT NULL,
    ip_address_enc  BYTEA,
    ip_address_hash TEXT,
    user_agent_enc  BYTEA,
    user_agent_hash TEXT,
    activity_time   TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
    description     TEXT,
    created_at      TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ(6)          DEFAULT now(),
    deleted_at      TIMESTAMPTZ(6),
    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_activity ON audit_logs (activity);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs (entity_type);
CREATE INDEX idx_audit_logs_activity_time ON audit_logs (activity_time);
CREATE INDEX idx_audit_logs_deleted_at ON audit_logs (deleted_at);
CREATE INDEX idx_audit_logs_not_deleted ON audit_logs (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_audit_logs_ip_hash ON audit_logs (ip_address_hash);
CREATE INDEX idx_audit_logs_ua_hash ON audit_logs (user_agent_hash);
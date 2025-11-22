CREATE TABLE users
(
    id                UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    username_enc      BYTEA          NOT NULL,
    username_hash     TEXT UNIQUE    NOT NULL,
    email_enc         BYTEA          NOT NULL,
    email_hash        TEXT UNIQUE    NOT NULL,
    password_hash     TEXT           NOT NULL,
    status            VARCHAR(50)    NOT NULL DEFAULT 'inactive',
    role              VARCHAR(50)    NOT NULL REFERENCES roles (code),
    is_email_verified BOOLEAN        NOT NULL DEFAULT FALSE,
    mfa_enabled       BOOLEAN        NOT NULL DEFAULT FALSE,
    last_login_at     TIMESTAMPTZ(6),
    created_at        TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ(6)
);


CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_created_at ON users (created_at);
CREATE INDEX idx_users_deleted_at ON users (deleted_at);
CREATE INDEX idx_users_not_deleted ON users (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_username_hash ON users (username_hash);
CREATE INDEX idx_users_email_hash ON users (email_hash);
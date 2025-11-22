CREATE TABLE roles
(
    code        VARCHAR(50) PRIMARY KEY,
    name        VARCHAR(100)   NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ(6) NOT NULL DEFAULT now()
);

CREATE INDEX idx_roles_created_at ON roles (created_at);
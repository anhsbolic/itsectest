CREATE TABLE user_profiles
(
    user_id        UUID PRIMARY KEY,
    full_name_enc  BYTEA          NOT NULL,
    full_name_hash TEXT           NOT NULL,
    avatar_url     TEXT,
    phone_enc      BYTEA,
    phone_hash     TEXT,
    created_at     TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ(6)          DEFAULT now(),
    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_profiles_full_name_hash ON user_profiles (full_name_hash);
CREATE INDEX idx_user_profiles_phone_hash ON user_profiles (phone_hash);
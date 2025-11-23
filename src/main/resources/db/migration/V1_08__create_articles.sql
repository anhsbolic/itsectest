CREATE TABLE articles
(
    id         UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    title      VARCHAR(255)   NOT NULL,
    content    TEXT           NOT NULL,
    author_id  UUID           NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ(6)          DEFAULT now(),
    deleted_at TIMESTAMPTZ(6),

    CONSTRAINT fk_articles_author
        FOREIGN KEY (author_id) REFERENCES users (id)
);

-- Indexes
CREATE INDEX idx_articles_author_id ON articles (author_id);
CREATE INDEX idx_articles_deleted_at ON articles (deleted_at);
CREATE INDEX idx_articles_not_deleted ON articles (deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_articles_title ON articles (title);
ALTER TABLE articles
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'draft';

CREATE INDEX idx_articles_status ON articles (status);
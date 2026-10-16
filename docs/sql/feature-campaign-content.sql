ALTER TABLE campaign_application
    ADD COLUMN IF NOT EXISTS experience_confirmed_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS campaign_content_submission (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE REFERENCES campaign_application(id),
    content_url VARCHAR(1000) NOT NULL,
    note TEXT,
    status VARCHAR(30) NOT NULL,
    revision_count INTEGER NOT NULL DEFAULT 0,
    review_comment TEXT,
    submitted_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_campaign_content_revision_count CHECK (revision_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_campaign_content_status
    ON campaign_content_submission(status, id DESC);

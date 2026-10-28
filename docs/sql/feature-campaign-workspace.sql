ALTER TABLE collaboration_campaign
    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(30);

CREATE TABLE IF NOT EXISTS campaign_content_link (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL REFERENCES campaign_content_submission(id) ON DELETE CASCADE,
    channel_type VARCHAR(30) NOT NULL,
    content_url VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_campaign_content_link_submission
    ON campaign_content_link(submission_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON campaign_content_link TO movrit_user;
GRANT USAGE, SELECT ON SEQUENCE campaign_content_link_id_seq TO movrit_user;

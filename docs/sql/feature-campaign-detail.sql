ALTER TABLE collaboration_campaign
    ADD COLUMN IF NOT EXISTS mission TEXT,
    ADD COLUMN IF NOT EXISTS channel_guide TEXT,
    ADD COLUMN IF NOT EXISTS schedule_guide TEXT,
    ADD COLUMN IF NOT EXISTS delivery_guide TEXT,
    ADD COLUMN IF NOT EXISTS visit_guide TEXT,
    ADD COLUMN IF NOT EXISTS caution TEXT,
    ADD COLUMN IF NOT EXISTS submission_requirement TEXT;

ALTER TABLE collaboration_campaign
    ADD COLUMN IF NOT EXISTS representative_image_url VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS detail_image_url VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS visit_place_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS visit_address VARCHAR(500);

CREATE TABLE IF NOT EXISTS campaign_detail_image (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES collaboration_campaign(id),
    image_url VARCHAR(1000) NOT NULL,
    display_order INTEGER NOT NULL,
    alt_text VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_campaign_detail_image_order UNIQUE (campaign_id, display_order)
);

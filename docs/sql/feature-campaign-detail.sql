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

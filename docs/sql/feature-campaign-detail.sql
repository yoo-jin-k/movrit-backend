ALTER TABLE collaboration_campaign
    ADD COLUMN IF NOT EXISTS mission TEXT,
    ADD COLUMN IF NOT EXISTS channel_guide TEXT,
    ADD COLUMN IF NOT EXISTS schedule_guide TEXT,
    ADD COLUMN IF NOT EXISTS delivery_guide TEXT,
    ADD COLUMN IF NOT EXISTS visit_guide TEXT,
    ADD COLUMN IF NOT EXISTS caution TEXT,
    ADD COLUMN IF NOT EXISTS submission_requirement TEXT;

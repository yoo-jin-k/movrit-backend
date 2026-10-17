ALTER TABLE collaboration_campaign
    ADD COLUMN IF NOT EXISTS selection_announced_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS experience_start_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS experience_end_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS content_submission_start_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS content_submission_end_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS results_announced_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS schedule_version BIGINT NOT NULL DEFAULT 1;

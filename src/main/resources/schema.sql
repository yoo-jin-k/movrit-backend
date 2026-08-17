ALTER TABLE influencer_profile
    ADD COLUMN IF NOT EXISTS channel_platform VARCHAR(30),
    ADD COLUMN IF NOT EXISTS channel_handle VARCHAR(100),
    ADD COLUMN IF NOT EXISTS channel_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS follower_count BIGINT,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);

UPDATE influencer_profile
SET status = 'PENDING'
WHERE status IS NULL
   OR status NOT IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED');

ALTER TABLE influencer_profile
    ALTER COLUMN status SET NOT NULL;

CREATE TABLE IF NOT EXISTS influencer_campaign (
    id BIGSERIAL PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    preorder_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    commission_rate NUMERIC(5, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_influencer_campaign_creator
        FOREIGN KEY (creator_id) REFERENCES influencer_profile(id),
    CONSTRAINT fk_influencer_campaign_preorder
        FOREIGN KEY (preorder_id) REFERENCES preorder(id),
    CONSTRAINT uk_influencer_campaign_creator_preorder
        UNIQUE (creator_id, preorder_id),
    CONSTRAINT ck_influencer_campaign_commission_rate
        CHECK (commission_rate >= 0 AND commission_rate <= 100)
);

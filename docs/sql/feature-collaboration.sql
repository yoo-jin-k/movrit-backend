ALTER TABLE influencer_profile ADD COLUMN IF NOT EXISTS sns_url VARCHAR(500);
ALTER TABLE influencer_profile ADD COLUMN IF NOT EXISTS follower_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS collaboration_campaign (
 id BIGSERIAL PRIMARY KEY, preorder_id BIGINT NOT NULL UNIQUE REFERENCES preorder(id), title VARCHAR(255) NOT NULL,
 description TEXT, category VARCHAR(100), min_follower_count BIGINT NOT NULL DEFAULT 0,
 recruitment_count INTEGER NOT NULL, reward_description TEXT, start_at TIMESTAMP NOT NULL,
 end_at TIMESTAMP NOT NULL, open BOOLEAN NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
 CONSTRAINT ck_campaign_followers CHECK (min_follower_count >= 0), CONSTRAINT ck_campaign_count CHECK (recruitment_count >= 1)
);
CREATE TABLE IF NOT EXISTS collaboration (
 id BIGSERIAL PRIMARY KEY, preorder_id BIGINT NOT NULL REFERENCES preorder(id), creator_id BIGINT NOT NULL REFERENCES influencer_profile(id),
 type VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL, message TEXT, referral_code VARCHAR(40) UNIQUE,
 responded_at TIMESTAMP, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
 CONSTRAINT uk_collaboration_preorder_creator UNIQUE(preorder_id, creator_id)
);
CREATE INDEX IF NOT EXISTS idx_collaboration_creator ON collaboration(creator_id);
CREATE INDEX IF NOT EXISTS idx_collaboration_preorder ON collaboration(preorder_id);
CREATE TABLE IF NOT EXISTS referral_click (
 id BIGSERIAL PRIMARY KEY, collaboration_id BIGINT NOT NULL REFERENCES collaboration(id), visitor_key VARCHAR(100),
 user_agent VARCHAR(500), created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_referral_click_collaboration ON referral_click(collaboration_id);

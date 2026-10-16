CREATE TABLE IF NOT EXISTS campaign_application_form (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL UNIQUE REFERENCES collaboration_campaign(id),
    allowed_fulfillment_types_json TEXT NOT NULL,
    questions_json TEXT NOT NULL,
    agreements_json TEXT NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS campaign_application (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES collaboration_campaign(id),
    creator_id BIGINT NOT NULL REFERENCES influencer_profile(id),
    fulfillment_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    message TEXT,
    recipient_name VARCHAR(100),
    recipient_phone VARCHAR(30),
    postal_code VARCHAR(20),
    address1 VARCHAR(300),
    address2 VARCHAR(300),
    answers_json TEXT NOT NULL,
    agreement_acceptances_json TEXT NOT NULL,
    form_version BIGINT NOT NULL,
    identity_verification_configured BOOLEAN NOT NULL,
    identity_verified BOOLEAN NOT NULL,
    channel_ready BOOLEAN NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_campaign_application_campaign_creator UNIQUE (campaign_id, creator_id),
    CONSTRAINT ck_campaign_application_visit_address CHECK (
        fulfillment_type <> 'VISIT'
        OR (recipient_name IS NULL AND recipient_phone IS NULL AND postal_code IS NULL
            AND address1 IS NULL AND address2 IS NULL)
    ),
    CONSTRAINT ck_campaign_application_delivery_address CHECK (
        fulfillment_type <> 'DELIVERY'
        OR (recipient_name IS NOT NULL AND recipient_phone IS NOT NULL
            AND postal_code IS NOT NULL AND address1 IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_campaign_application_campaign
    ON campaign_application(campaign_id);

CREATE INDEX IF NOT EXISTS idx_campaign_application_creator
    ON campaign_application(creator_id);

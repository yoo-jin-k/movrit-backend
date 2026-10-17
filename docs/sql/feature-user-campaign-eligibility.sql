CREATE TABLE IF NOT EXISTS member_channel_metric (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member(id),
    channel_type VARCHAR(40) NOT NULL,
    metric_type VARCHAR(40) NOT NULL,
    metric_value BIGINT NOT NULL,
    measurement_period VARCHAR(40),
    measured_at TIMESTAMP,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_member_channel_metric UNIQUE (member_id, channel_type, metric_type),
    CONSTRAINT ck_member_channel_metric_value CHECK (metric_value >= 0),
    CONSTRAINT ck_member_channel_metric_status CHECK (
        verification_status IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')
    )
);

CREATE INDEX IF NOT EXISTS idx_member_channel_metric_member
    ON member_channel_metric(member_id);

CREATE TABLE IF NOT EXISTS campaign_eligibility_requirement (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL UNIQUE REFERENCES collaboration_campaign(id),
    channel_type VARCHAR(40) NOT NULL,
    metric_type VARCHAR(40) NOT NULL,
    minimum_value BIGINT NOT NULL,
    measurement_period VARCHAR(40),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_campaign_eligibility_minimum CHECK (minimum_value >= 0)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON member_channel_metric TO movrit_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON campaign_eligibility_requirement TO movrit_user;
GRANT USAGE, SELECT ON SEQUENCE member_channel_metric_id_seq TO movrit_user;
GRANT USAGE, SELECT ON SEQUENCE campaign_eligibility_requirement_id_seq TO movrit_user;

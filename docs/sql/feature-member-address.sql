CREATE TABLE IF NOT EXISTS member_address (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member(id),
    address_name VARCHAR(30),
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    address1 VARCHAR(300) NOT NULL,
    address2 VARCHAR(300),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_member_address_member ON member_address(member_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_member_address_default
    ON member_address(member_id) WHERE is_default = TRUE;

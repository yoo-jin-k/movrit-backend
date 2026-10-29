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

-- 기존 설치본은 상세 주소가 VARCHAR(255)일 수 있으므로 저장된 값을 유지한 채 API 계약인 300자로 확장합니다.
ALTER TABLE member_address ALTER COLUMN address1 TYPE VARCHAR(300);
ALTER TABLE member_address ALTER COLUMN address2 TYPE VARCHAR(300);

CREATE INDEX IF NOT EXISTS idx_member_address_member ON member_address(member_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_member_address_default
    ON member_address(member_id) WHERE is_default = TRUE;

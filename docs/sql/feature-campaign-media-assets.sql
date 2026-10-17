-- 캠페인 미연결 자산/표시정보 신규 스키마입니다. 2026-10-18 로컬 movrit_integration에만 승인 적용했으며 다른 환경은 미적용입니다.
-- 프리오더 테이블·가격·주문 이력에는 접근하지 않습니다.
BEGIN;
CREATE TABLE IF NOT EXISTS campaign_image_asset (
    id VARCHAR(36) PRIMARY KEY,
    owner_member_id BIGINT NOT NULL REFERENCES member(id),
    campaign_id BIGINT REFERENCES collaboration_campaign(id) ON DELETE SET NULL,
    storage_key VARCHAR(100) NOT NULL UNIQUE,
    content_type VARCHAR(30) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size > 0),
    width INTEGER NOT NULL CHECK (width BETWEEN 1 AND 8192),
    height INTEGER NOT NULL CHECK (height BETWEEN 1 AND 8192),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CHECK (width::BIGINT * height <= 25000000)
);
CREATE INDEX IF NOT EXISTS idx_campaign_image_asset_campaign ON campaign_image_asset(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_image_asset_owner ON campaign_image_asset(owner_member_id);
CREATE TABLE IF NOT EXISTS campaign_media_state (
    campaign_id BIGINT PRIMARY KEY REFERENCES collaboration_campaign(id) ON DELETE CASCADE,
    version BIGINT NOT NULL CHECK (version >= 1),
    representative_asset_id VARCHAR(36) REFERENCES campaign_image_asset(id),
    detail_images_json TEXT NOT NULL,
    visit_location_json TEXT,
    retained_visit_location_json TEXT,
    legacy_representative_url VARCHAR(1000),
    legacy_detail_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
COMMIT;

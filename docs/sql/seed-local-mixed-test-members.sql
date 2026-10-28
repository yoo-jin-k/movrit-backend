-- 로컬 통합 화면에서 사용자·판매자 권한과 채널 지표 경계를 함께 확인하는 합성 데이터입니다.
-- 실행 전에 psql 변수 qa_password_hash에 BCrypt 해시를 전달해야 하며 평문 비밀번호는 저장하지 않습니다.
-- 이메일 확인 및 채널 지표 상태는 외부 검증 결과가 아닌 로컬 테스트 전용 상태입니다.

INSERT INTO member (
    email, password_hash, name, role, status, email_verified_at, created_at, updated_at
)
SELECT fixture.email, :'qa_password_hash', fixture.name, 'USER', 'ACTIVE',
       CASE WHEN fixture.email_verified THEN NOW() ELSE NULL END, NOW(), NOW()
FROM (VALUES
    ('qa-mixed-user-none-integration@movrit.local', 'QA 일반 조건없음', FALSE),
    ('qa-mixed-user-insta-low-integration@movrit.local', 'QA 인스타 320', TRUE),
    ('qa-mixed-user-insta-high-integration@movrit.local', 'QA 인스타 1250', TRUE),
    ('qa-mixed-user-insta-unverified-integration@movrit.local', 'QA 인스타 미확인', FALSE),
    ('qa-mixed-user-blog-integration@movrit.local', 'QA 블로그 방문자', TRUE),
    ('qa-mixed-user-youtube-integration@movrit.local', 'QA 유튜브 구독자', TRUE),
    ('qa-mixed-brand-alpha-integration@movrit.local', 'QA 브랜드 알파', TRUE),
    ('qa-mixed-brand-beta-integration@movrit.local', 'QA 브랜드 베타', TRUE),
    ('qa-mixed-brand-gamma-integration@movrit.local', 'QA 브랜드 감마', TRUE),
    ('qa-mixed-brand-pending-integration@movrit.local', 'QA 브랜드 심사대기', TRUE)
) AS fixture(email, name, email_verified)
ON CONFLICT (email) DO NOTHING;

INSERT INTO member_channel_metric (
    member_id, channel_type, metric_type, metric_value, measurement_period,
    measured_at, verification_status, created_at, updated_at
)
SELECT member.id, fixture.channel_type, fixture.metric_type, fixture.metric_value,
       fixture.measurement_period, NOW(), fixture.verification_status, NOW(), NOW()
FROM (VALUES
    ('qa-mixed-user-insta-low-integration@movrit.local', 'INSTAGRAM', 'FOLLOWER_COUNT', 320::bigint, NULL, 'VERIFIED'),
    ('qa-mixed-user-insta-high-integration@movrit.local', 'INSTAGRAM', 'FOLLOWER_COUNT', 1250::bigint, NULL, 'VERIFIED'),
    ('qa-mixed-user-insta-unverified-integration@movrit.local', 'INSTAGRAM', 'FOLLOWER_COUNT', 780::bigint, NULL, 'UNVERIFIED'),
    ('qa-mixed-user-blog-integration@movrit.local', 'NAVER_BLOG', 'MONTHLY_VISITORS', 2400::bigint, 'LAST_30_DAYS', 'VERIFIED'),
    ('qa-mixed-user-youtube-integration@movrit.local', 'YOUTUBE', 'SUBSCRIBER_COUNT', 600::bigint, NULL, 'VERIFIED')
) AS fixture(email, channel_type, metric_type, metric_value, measurement_period, verification_status)
JOIN member ON member.email = fixture.email
ON CONFLICT (member_id, channel_type, metric_type) DO UPDATE SET
    metric_value = EXCLUDED.metric_value,
    measurement_period = EXCLUDED.measurement_period,
    measured_at = EXCLUDED.measured_at,
    verification_status = EXCLUDED.verification_status,
    updated_at = NOW();

INSERT INTO seller_profile (
    member_id, business_name, brand_name, business_number, representative_name,
    status, created_at, updated_at
)
SELECT member.id, fixture.business_name, fixture.brand_name, fixture.business_number,
       fixture.representative_name, fixture.status, NOW(), NOW()
FROM (VALUES
    ('qa-mixed-brand-alpha-integration@movrit.local', '알파 스튜디오', 'QA ALPHA', 'QA-MIXED-ALPHA', '김알파', 'APPROVED'),
    ('qa-mixed-brand-beta-integration@movrit.local', '베타 리빙', 'QA BETA', 'QA-MIXED-BETA', '이베타', 'APPROVED'),
    ('qa-mixed-brand-gamma-integration@movrit.local', '감마 뷰티랩', 'QA GAMMA', 'QA-MIXED-GAMMA', '박감마', 'APPROVED'),
    ('qa-mixed-brand-pending-integration@movrit.local', '델타 메이커스', 'QA DELTA', 'QA-MIXED-DELTA', '최델타', 'PENDING')
) AS fixture(email, business_name, brand_name, business_number, representative_name, status)
JOIN member ON member.email = fixture.email
ON CONFLICT (member_id) DO UPDATE SET
    business_name = EXCLUDED.business_name,
    brand_name = EXCLUDED.brand_name,
    business_number = EXCLUDED.business_number,
    representative_name = EXCLUDED.representative_name,
    status = EXCLUDED.status,
    updated_at = NOW();

-- 승인된 판매자만 브랜드를 가집니다. 심사 대기 계정은 승인 전 화면과 권한 검수에 사용합니다.
INSERT INTO brand (seller_id, name, description, status, created_at, updated_at)
SELECT seller.id, seller.brand_name, '로컬 혼합 계정 검수용 합성 브랜드', 'ACTIVE', NOW(), NOW()
FROM seller_profile seller
WHERE seller.business_number IN ('QA-MIXED-ALPHA', 'QA-MIXED-BETA', 'QA-MIXED-GAMMA')
  AND seller.status = 'APPROVED'
  AND NOT EXISTS (
      SELECT 1 FROM brand
      WHERE brand.seller_id = seller.id AND brand.name = seller.brand_name
  );

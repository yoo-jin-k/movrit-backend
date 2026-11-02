-- 반응형·디자인 점검 전용 합성 계정입니다. 기존 사용자·QA 계정과 데이터를 변경하지 않습니다.
-- 실행 전에 psql 변수 qa_password_hash에 BCrypt 해시를 전달해야 하며 평문 비밀번호는 저장하지 않습니다.
-- 이메일 확인과 채널 지표 VERIFIED 상태는 외부 검증 결과가 아닌 로컬 점검용 상태입니다.

-- 1) 일반 사용자와 브랜드 담당자 계정을 만듭니다. 이미 있으면 건너뜁니다.
INSERT INTO member (
    email, password_hash, name, nickname, role, status, email_verified_at, created_at, updated_at
)
SELECT fixture.email, :'qa_password_hash', fixture.name, fixture.nickname, 'USER', 'ACTIVE', NOW(), NOW(), NOW()
FROM (VALUES
    ('demo-creator@movrit.local', '디자인점검 사용자', '점검사용자'),
    ('demo-brand@movrit.local', '디자인점검 브랜드', '점검브랜드')
) AS fixture(email, name, nickname)
ON CONFLICT (email) DO NOTHING;

-- 2) 사용자에게 조건 비교 화면을 확인할 인스타그램 지표를 등록합니다.
INSERT INTO member_channel_metric (
    member_id, channel_type, metric_type, metric_value, measurement_period,
    measured_at, verification_status, created_at, updated_at
)
SELECT member.id, 'INSTAGRAM', 'FOLLOWER_COUNT', 1500, NULL, NOW(), 'VERIFIED', NOW(), NOW()
FROM member
WHERE member.email = 'demo-creator@movrit.local'
ON CONFLICT (member_id, channel_type, metric_type) DO NOTHING;

-- 3) 브랜드 담당자는 승인된 판매자 프로필과 브랜드를 가져야 업체 화면에 들어갈 수 있습니다.
INSERT INTO seller_profile (
    member_id, business_name, brand_name, business_number, representative_name,
    status, created_at, updated_at
)
SELECT member.id, '디자인점검 스튜디오', 'QA DESIGN', 'QA-DEMO-BRAND', '김점검', 'APPROVED', NOW(), NOW()
FROM member
WHERE member.email = 'demo-brand@movrit.local'
ON CONFLICT (member_id) DO NOTHING;

INSERT INTO brand (seller_id, name, description, status, created_at, updated_at)
SELECT seller.id, seller.brand_name, '반응형·디자인 점검용 합성 브랜드', 'ACTIVE', NOW(), NOW()
FROM seller_profile seller
WHERE seller.business_number = 'QA-DEMO-BRAND'
  AND NOT EXISTS (SELECT 1 FROM brand WHERE brand.seller_id = seller.id);

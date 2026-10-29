-- 계정 설정 API를 실제 사용자 데이터와 분리해 검수하는 합성 계정입니다.
-- 평문 비밀번호는 저장하지 않고 기존 혼합 QA 계정의 해시만 재사용합니다.

INSERT INTO member (
    email, password_hash, name, nickname, role, status,
    email_verified_at, created_at, updated_at
)
SELECT fixture.email,
       CASE WHEN fixture.social_only THEN NULL ELSE source.password_hash END,
       fixture.name, fixture.nickname, 'USER', 'ACTIVE', NOW(), NOW(), NOW()
FROM member source
CROSS JOIN (VALUES
    ('qa-account-settings-integration@movrit.local', '계정 설정 QA', 'qa_account_settings', FALSE),
    ('qa-password-settings-integration@movrit.local', '비밀번호 설정 QA', 'qa_password_settings', FALSE),
    ('qa-social-only-settings-integration@movrit.local', '소셜 설정 QA', 'qa_social_settings', TRUE)
) AS fixture(email, name, nickname, social_only)
WHERE source.email = 'qa-mixed-user-insta-low-integration@movrit.local'
ON CONFLICT (email) DO NOTHING;

INSERT INTO member_address (
    member_id, address_name, receiver_name, receiver_phone,
    postal_code, address1, address2, is_default, created_at, updated_at
)
SELECT member.id, fixture.label, fixture.recipient_name, fixture.recipient_phone,
       fixture.postal_code, fixture.address1, fixture.address2, fixture.is_default, NOW(), NOW()
FROM member
CROSS JOIN (VALUES
    ('집', '계정 설정 사용자', '010-0000-0601', '06001', '서울특별시 테스트구 설정로 1', '101호', TRUE),
    ('회사', '계정 설정 사용자', '010-0000-0602', '06002', '서울특별시 테스트구 설정로 2', NULL, FALSE)
) AS fixture(label, recipient_name, recipient_phone, postal_code, address1, address2, is_default)
WHERE member.email = 'qa-account-settings-integration@movrit.local'
  AND NOT EXISTS (
      SELECT 1 FROM member_address existing
      WHERE existing.member_id = member.id AND existing.address_name = fixture.label
  );

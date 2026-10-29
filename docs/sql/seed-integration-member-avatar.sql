-- 실제 사용자와 분리된 프로필 사진 등록·교체·삭제 검수 계정입니다.
INSERT INTO member (
    email, password_hash, name, nickname, role, status,
    email_verified_at, avatar_version, created_at, updated_at
)
SELECT fixture.email, source.password_hash,
       fixture.name, fixture.nickname, 'USER', 'ACTIVE', NOW(), 0, NOW(), NOW()
FROM member source
CROSS JOIN (VALUES
    ('qa-member-avatar-fe-integration@movrit.local', '프로필 사진 FE', 'qa_avatar_fe'),
    ('qa-member-avatar-api-integration@movrit.local', '프로필 사진 API', 'qa_avatar_api')
) AS fixture(email, name, nickname)
WHERE source.email = 'qa-mixed-user-insta-low-integration@movrit.local'
ON CONFLICT (email) DO NOTHING;

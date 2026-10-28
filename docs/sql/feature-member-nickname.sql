-- 회원 실명·채널 프로필 이름과 분리된 공개 표시용 닉네임입니다.
ALTER TABLE member ADD COLUMN IF NOT EXISTS nickname VARCHAR(20);

-- 기존 회원은 실명을 복사하지 않고 식별자 기반의 비개인 대체 표시명을 사용합니다.
UPDATE member
SET nickname = 'user_' || id
WHERE nickname IS NULL OR BTRIM(nickname) = '';

ALTER TABLE member ALTER COLUMN nickname SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_member_nickname ON member(nickname);

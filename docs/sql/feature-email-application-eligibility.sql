-- 실명 인증과 구분해 신청 당시 이메일 소유 확인 상태를 보존합니다.
ALTER TABLE campaign_application
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 기존 테이블의 ID 시퀀스 기본값을 복구하여 JPA IDENTITY 전략과 맞춥니다.
ALTER TABLE notification ALTER COLUMN id SET DEFAULT nextval('notification_id_seq');
ALTER TABLE inquiry ALTER COLUMN id SET DEFAULT nextval('inquiry_id_seq');
ALTER TABLE audit_log ALTER COLUMN id SET DEFAULT nextval('audit_log_id_seq');

CREATE INDEX IF NOT EXISTS idx_notification_member_read
    ON notification(member_id, is_read, id DESC);

CREATE INDEX IF NOT EXISTS idx_inquiry_member
    ON inquiry(member_id, id DESC);

CREATE INDEX IF NOT EXISTS idx_inquiry_status
    ON inquiry(status, id DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_target
    ON audit_log(target_type, target_id, id DESC);

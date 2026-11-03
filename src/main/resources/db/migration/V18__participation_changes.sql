-- 참여 조정 기능을 추가합니다. 2026-11-03
-- 1) 신청 건별 콘텐츠 제출 마감 연장
-- 2) 선착순 선정 방식(방문형)
-- 3) 참여자 취소 횟수(1년 뒤 소멸, 5회면 패널티 1회)
-- 4) 협의 취소 요청(증빙 첨부 → 운영자 확인)
-- 5) 기간 연장 요청(참여자 요청 → 업체 승인, 신청당 1회)
-- 6) 업체 미선정 기록(선정 마감까지 아무도 선정하지 않은 캠페인)

-- 1) 연장이 승인되면 이 신청의 제출 마감만 늦춥니다. 비어 있으면 캠페인 마감을 그대로 씁니다.
ALTER TABLE campaign_application ADD COLUMN content_deadline_extended_until timestamp(6);
-- 연장된 마감 기준 알림은 따로 한 번씩 보냅니다(XD3·XD1). 지난 발송 기록은 지우지 않습니다.
ALTER TABLE campaign_deadline_reminder DROP CONSTRAINT IF EXISTS ck_campaign_deadline_reminder_stage;
ALTER TABLE campaign_deadline_reminder ADD CONSTRAINT ck_campaign_deadline_reminder_stage
  CHECK (stage IN ('D3', 'D1', 'XD3', 'XD1'));

-- 2) 선정 방식에 선착순(FIRST_COME)을 더합니다.
ALTER TABLE collaboration_campaign DROP CONSTRAINT IF EXISTS ck_campaign_selection_mode;
ALTER TABLE collaboration_campaign ADD CONSTRAINT ck_campaign_selection_mode
  CHECK (selection_mode IN ('BRAND_DIRECT', 'OPERATOR', 'FIRST_COME'));

-- 3) 취소 횟수가 쌓여 생기는 패널티 종류(CANCEL_LIMIT)를 더합니다.
ALTER TABLE member_penalty DROP CONSTRAINT IF EXISTS ck_member_penalty_type;
ALTER TABLE member_penalty ADD CONSTRAINT ck_member_penalty_type
  CHECK (penalty_type IN ('NO_SUBMIT', 'NO_SHOW', 'CANCEL_LIMIT'));

-- 취소 횟수: 신청 한 건에 한 번만 셉니다. 패널티로 바뀌면 penalty_id를 채워 다시 세지 않습니다.
CREATE TABLE member_cancel_count (
  id bigserial PRIMARY KEY,
  member_id bigint NOT NULL REFERENCES member (id),
  application_id bigint NOT NULL REFERENCES campaign_application (id),
  reason_type varchar(20) NOT NULL,
  created_at timestamp(6) NOT NULL,
  penalty_id bigint REFERENCES member_penalty (id),
  CONSTRAINT uk_member_cancel_count_application UNIQUE (application_id),
  CONSTRAINT ck_member_cancel_count_reason CHECK (reason_type IN ('SELF_CANCEL', 'MISSED_DEADLINE'))
);
CREATE INDEX ix_member_cancel_count_member ON member_cancel_count (member_id, created_at);

-- 4) 협의 취소 요청: 신청 한 건에 확인 중인 요청은 하나만 둡니다. 증빙 이미지는 비공개 폴더에 저장합니다.
CREATE TABLE campaign_cancel_request (
  id bigserial PRIMARY KEY,
  application_id bigint NOT NULL REFERENCES campaign_application (id),
  member_id bigint NOT NULL REFERENCES member (id),
  reason varchar(500) NOT NULL,
  evidence_key varchar(100) NOT NULL,
  status varchar(20) NOT NULL,
  decision_note varchar(500),
  decided_by_member_id bigint REFERENCES member (id),
  decided_at timestamp(6),
  created_at timestamp(6) NOT NULL,
  CONSTRAINT ck_campaign_cancel_request_status CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED'))
);
CREATE UNIQUE INDEX uk_campaign_cancel_request_open
  ON campaign_cancel_request (application_id) WHERE status = 'REQUESTED';
CREATE INDEX ix_campaign_cancel_request_status ON campaign_cancel_request (status, created_at);

-- 5) 기간 연장 요청: 신청당 한 번만 요청할 수 있습니다(반려되어도 다시 요청하지 않습니다).
CREATE TABLE campaign_extension_request (
  application_id bigint PRIMARY KEY REFERENCES campaign_application (id),
  member_id bigint NOT NULL REFERENCES member (id),
  days integer NOT NULL,
  reason varchar(500) NOT NULL,
  previous_deadline timestamp(6) NOT NULL,
  status varchar(20) NOT NULL,
  decision_note varchar(500),
  decided_by_member_id bigint REFERENCES member (id),
  decided_at timestamp(6),
  created_at timestamp(6) NOT NULL,
  CONSTRAINT ck_campaign_extension_request_days CHECK (days IN (7, 14)),
  CONSTRAINT ck_campaign_extension_request_status CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED'))
);

-- 6) 업체 미선정 기록: 캠페인마다 한 번만 남깁니다.
CREATE TABLE campaign_unselected_record (
  campaign_id bigint PRIMARY KEY REFERENCES collaboration_campaign (id),
  seller_member_id bigint NOT NULL REFERENCES member (id),
  application_count integer NOT NULL,
  recorded_at timestamp(6) NOT NULL
);
CREATE INDEX ix_campaign_unselected_record_seller ON campaign_unselected_record (seller_member_id, recorded_at);

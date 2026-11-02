-- 참여 운영 기능을 추가합니다. 2026-11-02
-- 1) 콘텐츠 마감 자동 알림(D-3·D-1) 발송 기록
-- 2) 방문형 캠페인 방문 예약
-- 3) 미제출·노쇼 패널티
-- 4) 완료 참여자 평가·메모(업체)
-- 5) 신고와 제재(콘텐츠·채팅·커뮤니티), 채팅 메시지 가림

-- 1) 마감 알림은 신청·단계(D3·D1)마다 한 번만 보냅니다.
CREATE TABLE campaign_deadline_reminder (
  id bigserial PRIMARY KEY,
  application_id bigint NOT NULL REFERENCES campaign_application (id),
  stage varchar(10) NOT NULL,
  sent_at timestamp(6) NOT NULL,
  CONSTRAINT uk_campaign_deadline_reminder UNIQUE (application_id, stage),
  CONSTRAINT ck_campaign_deadline_reminder_stage CHECK (stage IN ('D3', 'D1'))
);

-- 2) 방문 예약(신청당 1건): 참여자가 희망 일시를 요청하고 업체가 확정하거나 반려합니다.
CREATE TABLE campaign_visit_reservation (
  application_id bigint PRIMARY KEY REFERENCES campaign_application (id),
  visit_at timestamp(6) NOT NULL,
  party_size integer NOT NULL DEFAULT 1 CHECK (party_size BETWEEN 1 AND 10),
  request_note varchar(500),
  status varchar(20) NOT NULL,
  reject_reason varchar(500),
  requested_at timestamp(6) NOT NULL,
  decided_at timestamp(6),
  CONSTRAINT ck_campaign_visit_reservation_status CHECK (status IN ('REQUESTED', 'CONFIRMED', 'REJECTED'))
);

-- 3) 패널티: 업체(또는 운영자)가 미제출·노쇼를 기록합니다. 운영자가 취소할 수 있습니다.
CREATE TABLE member_penalty (
  id bigserial PRIMARY KEY,
  member_id bigint NOT NULL REFERENCES member (id),
  application_id bigint NOT NULL REFERENCES campaign_application (id),
  penalty_type varchar(20) NOT NULL,
  reason varchar(500) NOT NULL,
  created_by_member_id bigint NOT NULL REFERENCES member (id),
  created_at timestamp(6) NOT NULL,
  revoked_at timestamp(6),
  revoked_by_member_id bigint REFERENCES member (id),
  revoke_reason varchar(500),
  CONSTRAINT uk_member_penalty_application UNIQUE (application_id),
  CONSTRAINT ck_member_penalty_type CHECK (penalty_type IN ('NO_SUBMIT', 'NO_SHOW'))
);
CREATE INDEX ix_member_penalty_member ON member_penalty (member_id, created_at);

-- 4) 완료 참여자 평가(1~5점)와 업체만 보는 메모(신청당 1건)
CREATE TABLE campaign_participant_rating (
  application_id bigint PRIMARY KEY REFERENCES campaign_application (id),
  member_id bigint NOT NULL REFERENCES member (id),
  rating integer NOT NULL CHECK (rating BETWEEN 1 AND 5),
  memo varchar(1000),
  rated_by_member_id bigint NOT NULL REFERENCES member (id),
  updated_at timestamp(6) NOT NULL
);
CREATE INDEX ix_campaign_participant_rating_member ON campaign_participant_rating (member_id);

-- 5) 신고: 같은 사람이 같은 대상을 두 번 신고할 수 없습니다. 신고 당시 내용과 작성자를 함께 남깁니다.
CREATE TABLE content_report (
  id bigserial PRIMARY KEY,
  reporter_member_id bigint NOT NULL REFERENCES member (id),
  target_type varchar(30) NOT NULL,
  target_id bigint NOT NULL,
  target_member_id bigint REFERENCES member (id),
  target_snapshot varchar(2000),
  reason varchar(30) NOT NULL,
  detail varchar(1000),
  status varchar(20) NOT NULL DEFAULT 'OPEN',
  resolution varchar(30),
  resolution_note varchar(500),
  handled_by_member_id bigint REFERENCES member (id),
  handled_at timestamp(6),
  created_at timestamp(6) NOT NULL,
  CONSTRAINT uk_content_report UNIQUE (reporter_member_id, target_type, target_id),
  CONSTRAINT ck_content_report_target CHECK (target_type IN ('COMMUNITY_POST', 'COMMUNITY_COMMENT', 'CHAT_MESSAGE', 'CAMPAIGN_CONTENT')),
  CONSTRAINT ck_content_report_reason CHECK (reason IN ('SPAM', 'ABUSE', 'SEXUAL', 'PRIVACY', 'FRAUD', 'OTHER')),
  CONSTRAINT ck_content_report_status CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'))
);
CREATE INDEX ix_content_report_status ON content_report (status, created_at);

-- 운영자가 가린 채팅 메시지는 내용 대신 안내 문구로 보여줍니다(원문은 신고 기록에 남습니다).
ALTER TABLE chat_message ADD COLUMN hidden_at timestamp(6);

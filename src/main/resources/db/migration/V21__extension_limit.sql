-- 기간 연장 횟수 기준: 1년 안의 연장이 5회가 되면 운영자에게 알리고, 10회가 되면 패널티(EXTENSION_LIMIT)를 남겨 30일 동안 신청을 막습니다.
-- 패널티로 바뀐 연장 10회는 penalty_id를 채워 다시 세지 않습니다(취소 횟수와 같은 방식).

-- 1) 연장 기록에 '바뀐 패널티'를 남길 칸
ALTER TABLE campaign_period_extension ADD COLUMN penalty_id bigint REFERENCES member_penalty (id);

-- 2) 패널티 종류에 연장 누적 추가
ALTER TABLE member_penalty DROP CONSTRAINT IF EXISTS ck_member_penalty_type;
ALTER TABLE member_penalty ADD CONSTRAINT ck_member_penalty_type
  CHECK (penalty_type IN ('NO_SUBMIT', 'NO_SHOW', 'CANCEL_LIMIT', 'EXTENSION_LIMIT'));

-- 3) 연장 누적 패널티는 연장한 신청(진행 중일 수 있음)에 붙으므로, 같은 신청에 나중에 미제출·노쇼가 기록될 수 있게
--    신청당 1건 제약을 '신청·종류당 1건'으로 바꿉니다. 미제출·노쇼 중복은 서비스에서 계속 막습니다.
ALTER TABLE member_penalty DROP CONSTRAINT IF EXISTS uk_member_penalty_application;
ALTER TABLE member_penalty ADD CONSTRAINT uk_member_penalty_application_type
  UNIQUE (application_id, penalty_type);

-- 참여자 기간 연장 요청 일수를 7·14일 고정에서 1~30일 직접 입력으로 넓힙니다.
-- 업체 직접 연장(campaign_period_extension)과 같은 범위입니다.
ALTER TABLE campaign_extension_request DROP CONSTRAINT ck_campaign_extension_request_days;
ALTER TABLE campaign_extension_request
  ADD CONSTRAINT ck_campaign_extension_request_days CHECK (days BETWEEN 1 AND 30);

COMMENT ON COLUMN campaign_extension_request.days IS '참여자가 요청한 연장 일수(1~30일)';

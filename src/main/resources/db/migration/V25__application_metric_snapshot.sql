-- 신청 당시 지표 기록: 조건이 있는 캠페인에 신청할 때 조건 지표의 값과 확인 시각을 신청에 함께 남깁니다.
-- - 네이버 블로그 일평균 방문자처럼 평소에는 공개하지 않는 지표는 신청한 뒤 다시 숨길 수 있으므로, 업체는 이 기록만 봅니다.
-- - 조건이 없는 캠페인 신청은 두 값이 비어 있습니다.
ALTER TABLE campaign_application
  ADD COLUMN eligibility_metric_value bigint,
  ADD COLUMN eligibility_metric_measured_at timestamp(6);

ALTER TABLE campaign_application
  ADD CONSTRAINT ck_campaign_application_metric_value CHECK (eligibility_metric_value IS NULL OR eligibility_metric_value >= 0);

COMMENT ON COLUMN campaign_application.eligibility_metric_value IS '신청 당시 조건 지표 값(예: 네이버 블로그 최근 일평균 방문자). 조건 없는 신청은 비어 있습니다.';
COMMENT ON COLUMN campaign_application.eligibility_metric_measured_at IS '신청 당시 조건 지표를 확인한 시각입니다.';

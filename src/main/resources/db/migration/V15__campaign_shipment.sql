-- 배송형 캠페인의 발송 정보(택배사·송장번호)를 기록합니다. 2026-11-02
-- 선정된 배송형 참여자마다 1건입니다. 참여자가 수령(체험 완료)을 확인하기 전까지 업체가 고칠 수 있습니다.
CREATE TABLE campaign_shipment (
  application_id bigint PRIMARY KEY REFERENCES campaign_application (id),
  -- 택배사 이름(CJ대한통운·우체국택배·한진택배·롯데택배·로젠택배·경동택배·기타)
  courier varchar(30) NOT NULL,
  -- 송장번호(영문·숫자·하이픈 6~30자)
  tracking_number varchar(30) NOT NULL,
  shipped_at timestamp(6) NOT NULL,
  updated_at timestamp(6) NOT NULL
);

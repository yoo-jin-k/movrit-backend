-- 캠페인 탐색 메뉴(제품·지역·기자단·프리미어)를 실제 데이터로 보여주기 위한 분류 컬럼을 추가합니다. 2026-10-31

-- 캠페인 유형: PRODUCT(제품·배송 체험), LOCAL(지역·방문 체험), REPORTER(기자단·원고형)
ALTER TABLE collaboration_campaign ADD COLUMN campaign_type varchar(20) NOT NULL DEFAULT 'PRODUCT';
ALTER TABLE collaboration_campaign
  ADD CONSTRAINT ck_collaboration_campaign_type CHECK (campaign_type IN ('PRODUCT', 'LOCAL', 'REPORTER'));
-- 지역 캠페인의 방문 지역(서울·경기 등 광역 단위). 제품·기자단은 비워 둡니다.
ALTER TABLE collaboration_campaign ADD COLUMN region varchar(20);
-- 주로 콘텐츠를 올릴 채널(콘텐츠 제출 채널과 같은 값). 정하지 않으면 비워 둡니다.
ALTER TABLE collaboration_campaign ADD COLUMN content_channel varchar(20);
ALTER TABLE collaboration_campaign
  ADD CONSTRAINT ck_collaboration_campaign_channel
  CHECK (content_channel IS NULL OR content_channel IN ('INSTAGRAM', 'BLOG', 'YOUTUBE', 'CLIP', 'OTHER'));
-- 운영자가 지정하는 프리미어 캠페인 여부입니다. 업체 화면에서는 바꿀 수 없습니다.
ALTER TABLE collaboration_campaign ADD COLUMN premier boolean NOT NULL DEFAULT false;

-- 기존 캠페인 분류: 신청서가 방문형만 허용하면 지역 캠페인으로 봅니다.
UPDATE collaboration_campaign c
SET campaign_type = 'LOCAL'
FROM campaign_application_form f
WHERE f.campaign_id = c.id
  AND f.allowed_fulfillment_types_json LIKE '%VISIT%'
  AND f.allowed_fulfillment_types_json NOT LIKE '%DELIVERY%';

-- 지역 캠페인의 방문 주소 첫 단어로 광역 지역을 채웁니다(예: "서울 강남구 ..." → 서울). 맞는 지역이 없으면 비워 둡니다.
UPDATE collaboration_campaign
SET region = CASE
  WHEN visit_address LIKE '서울%' THEN '서울'
  WHEN visit_address LIKE '경기%' THEN '경기'
  WHEN visit_address LIKE '인천%' THEN '인천'
  WHEN visit_address LIKE '부산%' THEN '부산'
  WHEN visit_address LIKE '대구%' THEN '대구'
  WHEN visit_address LIKE '대전%' THEN '대전'
  WHEN visit_address LIKE '광주%' THEN '광주'
  WHEN visit_address LIKE '울산%' THEN '울산'
  WHEN visit_address LIKE '강원%' THEN '강원'
  WHEN visit_address LIKE '충%' THEN '충청'
  WHEN visit_address LIKE '전%' THEN '전라'
  WHEN visit_address LIKE '경상%' OR visit_address LIKE '경북%' OR visit_address LIKE '경남%' THEN '경상'
  WHEN visit_address LIKE '제주%' THEN '제주'
END
WHERE campaign_type = 'LOCAL' AND visit_address IS NOT NULL;

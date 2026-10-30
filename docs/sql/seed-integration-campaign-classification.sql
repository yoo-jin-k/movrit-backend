-- 통합 시연 데이터: 샘플 카탈로그 캠페인(20~31)에 유형·주요 채널·프리미어 분류를 지정합니다. 2026-10-31
-- 탐색 메뉴(기자단·프리미어·미디어 필터)가 빈 화면으로 보이지 않게 하기 위한 합성 분류이며 신청·콘텐츠 데이터는 바꾸지 않습니다.
-- V4 마이그레이션 이후 통합 DB(movrit_integration)에서만 수동으로 실행합니다. 여러 번 실행해도 결과가 같습니다.

-- 원고·공간 소개형 캠페인은 기자단으로 분류합니다.
UPDATE collaboration_campaign SET campaign_type = 'REPORTER', region = NULL WHERE id IN (24, 25);

-- 방문형 샘플의 지역 세부 분류를 지역 메뉴 분류값에 맞춥니다.
UPDATE collaboration_campaign SET category = '기타' WHERE id = 27 AND campaign_type = 'LOCAL';
UPDATE collaboration_campaign SET category = '문화' WHERE id = 28 AND campaign_type = 'LOCAL';

-- 주로 콘텐츠를 올릴 채널(미디어 필터)입니다.
UPDATE collaboration_campaign SET content_channel = 'BLOG' WHERE id IN (20, 22, 24, 25, 28);
UPDATE collaboration_campaign SET content_channel = 'INSTAGRAM' WHERE id IN (21, 23, 26, 29);
UPDATE collaboration_campaign SET content_channel = 'CLIP' WHERE id = 27;
UPDATE collaboration_campaign SET content_channel = 'YOUTUBE' WHERE id IN (30, 31);

-- 운영자가 지정한 프리미어 캠페인입니다.
UPDATE collaboration_campaign SET premier = true WHERE id IN (20, 30);

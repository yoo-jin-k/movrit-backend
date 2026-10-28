-- 로컬 통합 화면에서 다양한 상품 캠페인을 조회하고 실제 신청 흐름을 검수하기 위한 합성 데이터입니다.
-- 상품명과 SKU를 기준으로 중복을 막으므로 같은 DB에 여러 번 실행해도 기존 행을 보존합니다.

WITH fixture(brand_name, product_name, category, normal_price) AS (
    VALUES
        ('QA ALPHA', '[DEMO] 저자극 수분 세럼', '뷰티', 32000::numeric),
        ('QA GAMMA', '[DEMO] 데일리 톤업 선크림', '뷰티', 28000::numeric),
        ('QA BETA',  '[DEMO] 통곡물 에너지바 세트', '식품', 24000::numeric),
        ('QA ALPHA', '[DEMO] 콜드브루 원액 키트', '식품', 29000::numeric),
        ('QA BETA',  '[DEMO] 모듈형 데스크 정리함', '생활', 36000::numeric),
        ('QA GAMMA', '[DEMO] 포근한 패브릭 미스트', '생활', 19000::numeric),
        ('QA ALPHA', '[DEMO] 데일리 경량 크로스백', '패션', 59000::numeric),
        ('QA BETA',  '[DEMO] 리사이클 러닝 캡', '패션', 33000::numeric),
        ('QA GAMMA', '[DEMO] 밸런스 요가 매트', '생활', 52000::numeric),
        ('QA ALPHA', '[DEMO] 트레일 러닝 보틀', '생활', 27000::numeric),
        ('QA BETA',  '[DEMO] 휴대용 무드 라이트', '디지털', 45000::numeric),
        ('QA GAMMA', '[DEMO] 미니 블루투스 스피커', '디지털', 69000::numeric)
)
INSERT INTO product (
    brand_id, name, description, category, normal_price, status,
    thumbnail_url, created_at, updated_at
)
SELECT brand.id, fixture.product_name,
       'MOVRIT 로컬 연동 검수용 가상 상품입니다. 실제 판매 또는 결제 대상이 아닙니다.',
       fixture.category, fixture.normal_price, 'ACTIVE',
       CASE fixture.category
           WHEN '뷰티' THEN '/assets/discovery/banners/review.png'
           WHEN '식품' THEN '/assets/discovery/banners/dining.png'
           WHEN '패션' THEN '/assets/discovery/banners/travel.png'
           WHEN '디지털' THEN '/assets/discovery/banners/clip.png'
           ELSE '/assets/discovery/banners/living.png'
       END,
       NOW(), NOW()
FROM fixture
JOIN brand ON brand.name = fixture.brand_name AND brand.status = 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM product existing
    WHERE existing.brand_id = brand.id AND existing.name = fixture.product_name
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM product_variant variant
        JOIN product ON product.id = variant.product_id
        JOIN (VALUES
            ('DEMO-BEAUTY-SERUM', '[DEMO] 저자극 수분 세럼'),
            ('DEMO-BEAUTY-SUN', '[DEMO] 데일리 톤업 선크림'),
            ('DEMO-FOOD-BAR', '[DEMO] 통곡물 에너지바 세트'),
            ('DEMO-FOOD-COLD', '[DEMO] 콜드브루 원액 키트'),
            ('DEMO-LIFE-DESK', '[DEMO] 모듈형 데스크 정리함'),
            ('DEMO-LIFE-MIST', '[DEMO] 포근한 패브릭 미스트'),
            ('DEMO-FASHION-BAG', '[DEMO] 데일리 경량 크로스백'),
            ('DEMO-FASHION-CAP', '[DEMO] 리사이클 러닝 캡'),
            ('DEMO-SPORT-YOGA', '[DEMO] 밸런스 요가 매트'),
            ('DEMO-SPORT-BOTTLE', '[DEMO] 트레일 러닝 보틀'),
            ('DEMO-DIGITAL-LIGHT', '[DEMO] 휴대용 무드 라이트'),
            ('DEMO-DIGITAL-SPEAKER', '[DEMO] 미니 블루투스 스피커')
        ) AS expected(sku_code, product_name) ON expected.sku_code = variant.sku_code
        WHERE product.name <> expected.product_name
    ) THEN
        RAISE EXCEPTION '데모 SKU가 다른 상품에 연결되어 있어 시드를 중단합니다.';
    END IF;
END $$;

WITH fixture(product_name, sku_code, option_value) AS (
    VALUES
        ('[DEMO] 저자극 수분 세럼', 'DEMO-BEAUTY-SERUM', '50ml'),
        ('[DEMO] 데일리 톤업 선크림', 'DEMO-BEAUTY-SUN', '50ml'),
        ('[DEMO] 통곡물 에너지바 세트', 'DEMO-FOOD-BAR', '12개입'),
        ('[DEMO] 콜드브루 원액 키트', 'DEMO-FOOD-COLD', '500ml'),
        ('[DEMO] 모듈형 데스크 정리함', 'DEMO-LIFE-DESK', '기본형'),
        ('[DEMO] 포근한 패브릭 미스트', 'DEMO-LIFE-MIST', '300ml'),
        ('[DEMO] 데일리 경량 크로스백', 'DEMO-FASHION-BAG', '블랙'),
        ('[DEMO] 리사이클 러닝 캡', 'DEMO-FASHION-CAP', '프리'),
        ('[DEMO] 밸런스 요가 매트', 'DEMO-SPORT-YOGA', '6mm'),
        ('[DEMO] 트레일 러닝 보틀', 'DEMO-SPORT-BOTTLE', '500ml'),
        ('[DEMO] 휴대용 무드 라이트', 'DEMO-DIGITAL-LIGHT', '화이트'),
        ('[DEMO] 미니 블루투스 스피커', 'DEMO-DIGITAL-SPEAKER', '차콜')
)
INSERT INTO product_variant (
    product_id, sku_code, option_name_1, option_value_1, additional_price,
    on_hand_quantity, reserved_quantity, safety_stock_quantity, sold_quantity,
    active, version, created_at, updated_at
)
SELECT product.id, fixture.sku_code, '구성', fixture.option_value, 0,
       100, 0, 10, 0, TRUE, 0, NOW(), NOW()
FROM fixture
JOIN product ON product.name = fixture.product_name
ON CONFLICT (sku_code) DO NOTHING;

INSERT INTO preorder (
    product_id, title, description, preorder_price, currency,
    target_quantity, max_quantity, ordered_quantity, start_at, end_at,
    production_expected_at, shipping_expected_at, status, version,
    created_at, updated_at, min_order_quantity, max_order_quantity_per_member
)
SELECT product.id, product.name || ' 체험 프리오더',
       '캠페인 신청 연결을 위한 가상 프리오더이며 실제 주문과 결제를 만들지 않습니다.',
       ROUND(product.normal_price * 0.8), 'KRW', 20, 200, 0,
       NOW() - INTERVAL '2 days', NOW() + INTERVAL '45 days',
       NOW() + INTERVAL '50 days', NOW() + INTERVAL '55 days',
       'OPEN', 0, NOW(), NOW(), 1, 2
FROM product
WHERE product.name IN (
    '[DEMO] 저자극 수분 세럼', '[DEMO] 데일리 톤업 선크림',
    '[DEMO] 통곡물 에너지바 세트', '[DEMO] 콜드브루 원액 키트',
    '[DEMO] 모듈형 데스크 정리함', '[DEMO] 포근한 패브릭 미스트',
    '[DEMO] 데일리 경량 크로스백', '[DEMO] 리사이클 러닝 캡',
    '[DEMO] 밸런스 요가 매트', '[DEMO] 트레일 러닝 보틀',
    '[DEMO] 휴대용 무드 라이트', '[DEMO] 미니 블루투스 스피커'
)
  AND NOT EXISTS (
      SELECT 1 FROM preorder existing WHERE existing.product_id = product.id
  );

WITH fixture(product_name, campaign_title, category, reward_description,
             fulfillment_type, minimum_followers, visit_place, visit_address) AS (
    VALUES
        ('[DEMO] 저자극 수분 세럼', '수분 세럼 7일 사용 리뷰어 모집', '뷰티', '가상 상품 1개와 제작 지원 안내', 'DELIVERY', 0::bigint, NULL, NULL),
        ('[DEMO] 데일리 톤업 선크림', '톤업 선크림 출근 루틴 체험단', '뷰티', '가상 상품 1개 제공', 'DELIVERY', 300::bigint, NULL, NULL),
        ('[DEMO] 통곡물 에너지바 세트', '건강 간식 일상 리뷰어 모집', '식품', '가상 체험 세트 제공', 'DELIVERY', 0::bigint, NULL, NULL),
        ('[DEMO] 콜드브루 원액 키트', '홈카페 콜드브루 레시피 캠페인', '식품', '가상 키트 제공', 'DELIVERY', 0::bigint, NULL, NULL),
        ('[DEMO] 모듈형 데스크 정리함', '책상 정리 전후 콘텐츠 모집', '생활', '가상 상품 1개 제공', 'DELIVERY', 0::bigint, NULL, NULL),
        ('[DEMO] 포근한 패브릭 미스트', '패브릭 미스트 공간 리뷰 캠페인', '생활', '가상 상품 1개 제공', 'DELIVERY', 0::bigint, NULL, NULL),
        ('[DEMO] 데일리 경량 크로스백', '경량 크로스백 데일리 코디 모집', '패션', '가상 상품 1개 제공', 'DELIVERY', 0::bigint, NULL, NULL),
        ('[DEMO] 리사이클 러닝 캡', '러닝 캡 착용 숏폼 캠페인', '패션', '현장 가상 샘플 대여', 'VISIT', 500::bigint, 'MOVRIT 러닝 스튜디오', '서울특별시 성동구 연무장길 1'),
        ('[DEMO] 밸런스 요가 매트', '요가 매트 클래스 체험단', '생활', '가상 클래스 참여', 'VISIT', 0::bigint, 'MOVRIT 밸런스룸', '서울특별시 마포구 월드컵북로 1'),
        ('[DEMO] 트레일 러닝 보틀', '러닝 보틀 야외 사용 리뷰 모집', '생활', '가상 상품 1개 제공', 'DELIVERY', 1000::bigint, NULL, NULL),
        ('[DEMO] 휴대용 무드 라이트', '무드 라이트 데스크 셋업 모집', '디지털', '가상 상품 1개 제공', 'DELIVERY', 0::bigint, NULL, NULL),
        ('[DEMO] 미니 블루투스 스피커', '미니 스피커 음향 체험단', '디지털', '가상 상품 1개 제공', 'DELIVERY', 0::bigint, NULL, NULL)
)
INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count,
    recruitment_count, reward_description, start_at, end_at, open,
    mission, channel_guide, schedule_guide, delivery_guide, visit_guide,
    caution, submission_requirement, selection_announced_at,
    experience_start_at, experience_end_at, content_submission_start_at,
    content_submission_end_at, results_announced_at, schedule_version,
    representative_image_url, detail_image_url, visit_place_name, visit_address,
    created_at, updated_at
)
SELECT preorder.id, fixture.campaign_title,
       '프론트와 실제 신청 API 연결을 확인하는 로컬 합성 캠페인입니다.',
       fixture.category, 0, 20, fixture.reward_description,
       NOW() - INTERVAL '1 day', NOW() + INTERVAL '21 days', TRUE,
       '상품을 직접 체험한 과정과 솔직한 사용 소감을 사진 또는 짧은 영상으로 소개해 주세요.',
       '인스타그램 피드·릴스 또는 블로그 중 본인의 주 채널에 공개해 주세요.',
       '선정 후 안내된 체험 기간과 제출 기간을 지켜 주세요.',
       CASE WHEN fixture.fulfillment_type = 'DELIVERY' THEN '선정된 테스트 주소로 가상 상품을 배송합니다.' END,
       CASE WHEN fixture.fulfillment_type = 'VISIT' THEN '선정 후 방문 일정을 별도로 안내합니다.' END,
       '검수용 가상 캠페인이므로 실제 결제·구매·효능 표현을 포함하지 마세요.',
       '공개 게시물 URL과 필수 해시태그를 제출해 주세요.',
       NOW() + INTERVAL '23 days', NOW() + INTERVAL '25 days', NOW() + INTERVAL '35 days',
       NOW() + INTERVAL '25 days', NOW() + INTERVAL '40 days', NOW() + INTERVAL '42 days', 1,
       CASE fixture.category
           WHEN '뷰티' THEN '/assets/discovery/banners/review.png'
           WHEN '식품' THEN '/assets/discovery/banners/dining.png'
           WHEN '패션' THEN '/assets/discovery/banners/travel.png'
           WHEN '디지털' THEN '/assets/discovery/banners/clip.png'
           ELSE '/assets/discovery/banners/living.png'
       END,
       CASE fixture.category
           WHEN '뷰티' THEN '/assets/discovery/banners/review.png'
           WHEN '식품' THEN '/assets/discovery/banners/dining.png'
           WHEN '패션' THEN '/assets/discovery/banners/travel.png'
           WHEN '디지털' THEN '/assets/discovery/banners/clip.png'
           ELSE '/assets/discovery/banners/living.png'
       END,
       fixture.visit_place, fixture.visit_address, NOW(), NOW()
FROM fixture
JOIN product ON product.name = fixture.product_name
JOIN preorder ON preorder.product_id = product.id
WHERE NOT EXISTS (
    SELECT 1 FROM collaboration_campaign existing WHERE existing.preorder_id = preorder.id
);

-- 재실행 시에도 데모 캠페인은 현재 시각 기준 모집 중 상태를 유지합니다.
UPDATE collaboration_campaign
SET start_at = NOW() - INTERVAL '1 day',
    end_at = NOW() + INTERVAL '21 days',
    open = TRUE,
    selection_announced_at = NOW() + INTERVAL '23 days',
    experience_start_at = NOW() + INTERVAL '25 days',
    experience_end_at = NOW() + INTERVAL '35 days',
    content_submission_start_at = NOW() + INTERVAL '25 days',
    content_submission_end_at = NOW() + INTERVAL '40 days',
    results_announced_at = NOW() + INTERVAL '42 days',
    updated_at = NOW()
WHERE title IN (
    '수분 세럼 7일 사용 리뷰어 모집', '톤업 선크림 출근 루틴 체험단',
    '건강 간식 일상 리뷰어 모집', '홈카페 콜드브루 레시피 캠페인',
    '책상 정리 전후 콘텐츠 모집', '패브릭 미스트 공간 리뷰 캠페인',
    '경량 크로스백 데일리 코디 모집', '러닝 캡 착용 숏폼 캠페인',
    '요가 매트 클래스 체험단', '러닝 보틀 야외 사용 리뷰 모집',
    '무드 라이트 데스크 셋업 모집', '미니 스피커 음향 체험단'
);

-- 초기 적용 뒤 이미지 계약을 보강해도 데모 12건만 출처가 확인된 프론트 로컬 자산으로 교정합니다.
UPDATE product
SET thumbnail_url = CASE category
        WHEN '뷰티' THEN '/assets/discovery/banners/review.png'
        WHEN '식품' THEN '/assets/discovery/banners/dining.png'
        WHEN '패션' THEN '/assets/discovery/banners/travel.png'
        WHEN '디지털' THEN '/assets/discovery/banners/clip.png'
        ELSE '/assets/discovery/banners/living.png'
    END,
    updated_at = NOW()
WHERE name IN (
    '[DEMO] 저자극 수분 세럼', '[DEMO] 데일리 톤업 선크림',
    '[DEMO] 통곡물 에너지바 세트', '[DEMO] 콜드브루 원액 키트',
    '[DEMO] 모듈형 데스크 정리함', '[DEMO] 포근한 패브릭 미스트',
    '[DEMO] 데일리 경량 크로스백', '[DEMO] 리사이클 러닝 캡',
    '[DEMO] 밸런스 요가 매트', '[DEMO] 트레일 러닝 보틀',
    '[DEMO] 휴대용 무드 라이트', '[DEMO] 미니 블루투스 스피커'
);

UPDATE collaboration_campaign campaign
SET representative_image_url = product.thumbnail_url,
    detail_image_url = product.thumbnail_url,
    updated_at = NOW()
FROM preorder
JOIN product ON product.id = preorder.product_id
WHERE campaign.preorder_id = preorder.id
  AND product.name IN (
      '[DEMO] 저자극 수분 세럼', '[DEMO] 데일리 톤업 선크림',
      '[DEMO] 통곡물 에너지바 세트', '[DEMO] 콜드브루 원액 키트',
      '[DEMO] 모듈형 데스크 정리함', '[DEMO] 포근한 패브릭 미스트',
      '[DEMO] 데일리 경량 크로스백', '[DEMO] 리사이클 러닝 캡',
      '[DEMO] 밸런스 요가 매트', '[DEMO] 트레일 러닝 보틀',
      '[DEMO] 휴대용 무드 라이트', '[DEMO] 미니 블루투스 스피커'
  );

WITH fixture(campaign_title, fulfillment_type) AS (
    VALUES
        ('수분 세럼 7일 사용 리뷰어 모집', 'DELIVERY'),
        ('톤업 선크림 출근 루틴 체험단', 'DELIVERY'),
        ('건강 간식 일상 리뷰어 모집', 'DELIVERY'),
        ('홈카페 콜드브루 레시피 캠페인', 'DELIVERY'),
        ('책상 정리 전후 콘텐츠 모집', 'DELIVERY'),
        ('패브릭 미스트 공간 리뷰 캠페인', 'DELIVERY'),
        ('경량 크로스백 데일리 코디 모집', 'DELIVERY'),
        ('러닝 캡 착용 숏폼 캠페인', 'VISIT'),
        ('요가 매트 클래스 체험단', 'VISIT'),
        ('러닝 보틀 야외 사용 리뷰 모집', 'DELIVERY'),
        ('무드 라이트 데스크 셋업 모집', 'DELIVERY'),
        ('미니 스피커 음향 체험단', 'DELIVERY')
)
INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json,
    agreements_json, version, created_at, updated_at
)
SELECT campaign.id, '["' || fixture.fulfillment_type || '"]',
       '[{"id":"experience","label":"관련 콘텐츠 경험을 알려 주세요.","required":true,"type":"TEXTAREA","options":[],"maxLength":1000},{"id":"plan","label":"제작할 콘텐츠 계획을 알려 주세요.","required":true,"type":"TEXTAREA","options":[],"maxLength":1000},{"id":"channel","label":"주로 사용할 채널을 선택해 주세요.","required":true,"type":"SINGLE_CHOICE","options":["인스타그램","블로그","유튜브"],"maxLength":20}]',
       '[{"id":"privacy","title":"테스트 신청 정보 수집 동의","version":"2026-10-29","required":true,"body":"로컬 캠페인 신청과 배송 또는 방문 안내 검수를 위해 입력 정보를 저장합니다."},{"id":"content-guide","title":"콘텐츠 제작 안내 확인","version":"2026-10-29","required":true,"body":"검수용 가상 상품임을 이해하고 실제 구매·결제·효능 표현을 작성하지 않습니다."}]',
       1, NOW(), NOW()
FROM fixture
JOIN collaboration_campaign campaign ON campaign.title = fixture.campaign_title
ON CONFLICT (campaign_id) DO NOTHING;

WITH fixture(campaign_title, minimum_value) AS (
    VALUES
        ('톤업 선크림 출근 루틴 체험단', 300::bigint),
        ('러닝 캡 착용 숏폼 캠페인', 500::bigint),
        ('러닝 보틀 야외 사용 리뷰 모집', 1000::bigint)
)
INSERT INTO campaign_eligibility_requirement (
    campaign_id, channel_type, metric_type, minimum_value,
    measurement_period, version, created_at, updated_at
)
SELECT campaign.id, 'INSTAGRAM', 'FOLLOWER_COUNT', fixture.minimum_value,
       NULL, 0, NOW(), NOW()
FROM fixture
JOIN collaboration_campaign campaign ON campaign.title = fixture.campaign_title
ON CONFLICT (campaign_id) DO UPDATE SET
    channel_type = EXCLUDED.channel_type,
    metric_type = EXCLUDED.metric_type,
    minimum_value = EXCLUDED.minimum_value,
    measurement_period = NULL,
    updated_at = NOW();

-- 실제 신청 검수에 사용하는 일반 계정에 채널 프로필과 검증된 지표를 연결합니다.
INSERT INTO influencer_profile (
    member_id, nickname, bio, primary_category, status,
    sns_url, follower_count, created_at, updated_at
)
SELECT member.id, fixture.nickname, '로컬 캠페인 신청 검수용 채널입니다.',
       fixture.category, 'ACTIVE', fixture.sns_url, fixture.followers, NOW(), NOW()
FROM (VALUES
    ('qa-mixed-user-insta-low-integration@movrit.local', '인스타 낮은지표', '뷰티', 'https://instagram.com/movrit_qa_low', 120::bigint),
    ('qa-mixed-user-insta-high-integration@movrit.local', '인스타 높은지표', '라이프', 'https://instagram.com/movrit_qa_high', 2500::bigint)
) AS fixture(email, nickname, category, sns_url, followers)
JOIN member ON member.email = fixture.email
ON CONFLICT (member_id) DO UPDATE SET
    nickname = EXCLUDED.nickname,
    status = 'ACTIVE',
    sns_url = EXCLUDED.sns_url,
    follower_count = EXCLUDED.follower_count,
    updated_at = NOW();

INSERT INTO member_channel_metric (
    member_id, channel_type, metric_type, metric_value,
    measurement_period, measured_at, verification_status, created_at, updated_at
)
SELECT member.id, 'INSTAGRAM', 'FOLLOWER_COUNT', fixture.followers,
       NULL, NOW(), 'VERIFIED', NOW(), NOW()
FROM (VALUES
    ('qa-mixed-user-insta-low-integration@movrit.local', 120::bigint),
    ('qa-mixed-user-insta-high-integration@movrit.local', 2500::bigint)
) AS fixture(email, followers)
JOIN member ON member.email = fixture.email
ON CONFLICT (member_id, channel_type, metric_type) DO UPDATE SET
    metric_value = EXCLUDED.metric_value,
    measured_at = EXCLUDED.measured_at,
    verification_status = 'VERIFIED',
    updated_at = NOW();

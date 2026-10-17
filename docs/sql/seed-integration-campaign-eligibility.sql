-- 프론트에서 조건 있음/없음과 확인 상태를 함께 검수하기 위한 통합 DB 전용 데이터입니다.
INSERT INTO campaign_eligibility_requirement (
    campaign_id, channel_type, metric_type, minimum_value, measurement_period, created_at, updated_at
)
SELECT 12, 'INSTAGRAM', 'FOLLOWER_COUNT', 500, NULL, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM collaboration_campaign WHERE id = 12)
ON CONFLICT (campaign_id) DO UPDATE SET
    channel_type = EXCLUDED.channel_type,
    metric_type = EXCLUDED.metric_type,
    minimum_value = EXCLUDED.minimum_value,
    measurement_period = EXCLUDED.measurement_period,
    updated_at = NOW();

-- 회원 12는 확인 완료, 회원 13은 사용자 입력 직후 상태를 보여 줍니다.
INSERT INTO member_channel_metric (
    member_id, channel_type, metric_type, metric_value, measurement_period,
    measured_at, verification_status, created_at, updated_at
)
SELECT fixture.member_id, 'INSTAGRAM', 'FOLLOWER_COUNT', fixture.metric_value, NULL,
       NOW(), fixture.verification_status, NOW(), NOW()
FROM (VALUES
    (12::bigint, 800::bigint, 'VERIFIED'),
    (13::bigint, 700::bigint, 'UNVERIFIED')
) AS fixture(member_id, metric_value, verification_status)
WHERE EXISTS (SELECT 1 FROM member WHERE id = fixture.member_id)
ON CONFLICT (member_id, channel_type, metric_type) DO UPDATE SET
    metric_value = EXCLUDED.metric_value,
    measurement_period = EXCLUDED.measurement_period,
    measured_at = EXCLUDED.measured_at,
    verification_status = EXCLUDED.verification_status,
    updated_at = NOW();

-- 499/500 경계를 다른 상태와 섞지 않고 HTTP로 확인하는 격리 계정입니다.
-- 비밀번호 해시는 기존 브라우저 QA 계정의 로컬 자격증명을 재사용하며 평문은 저장하지 않습니다.
INSERT INTO member (email, password_hash, name, role, status, email_verified_at, created_at, updated_at)
SELECT fixture.email, source.password_hash, fixture.name, 'USER', 'ACTIVE', NOW(), NOW(), NOW()
FROM member source
CROSS JOIN (VALUES
    ('qa-metric-499-integration@movrit.local', '지표 경계 499 검수'),
    ('qa-metric-500-integration@movrit.local', '지표 경계 500 검수')
) AS fixture(email, name)
WHERE source.email = 'qa-browser-integration@movrit.local'
ON CONFLICT (email) DO NOTHING;

INSERT INTO preorder (
    product_id, title, description, preorder_price, currency, target_quantity, max_quantity,
    ordered_quantity, start_at, end_at, production_expected_at, shipping_expected_at,
    status, version, created_at, updated_at, min_order_quantity, max_order_quantity_per_member
)
SELECT source.product_id, '[QA] 채널 지표 경계 검수 프리오더', '통합 API 경계 검수 전용 데이터',
       source.preorder_price, source.currency, 10, 20, 0,
       NOW() - INTERVAL '1 day', NOW() + INTERVAL '14 days',
       NOW() + INTERVAL '21 days', NOW() + INTERVAL '28 days',
       'OPEN', 0, NOW(), NOW(), 1, 1
FROM preorder source
WHERE source.id = 9
  AND NOT EXISTS (SELECT 1 FROM preorder WHERE title = '[QA] 채널 지표 경계 검수 프리오더');

INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count, recruitment_count,
    reward_description, start_at, end_at, open, created_at, updated_at, schedule_version
)
SELECT preorder.id, '[QA] 채널 지표 499/500 경계 검수', '통합 API 자격 판정 전용 캠페인',
       'QA', 0, 10, '검수 전용', NOW() - INTERVAL '1 day', NOW() + INTERVAL '7 days',
       TRUE, NOW(), NOW(), 1
FROM preorder
WHERE preorder.title = '[QA] 채널 지표 경계 검수 프리오더'
  AND NOT EXISTS (SELECT 1 FROM collaboration_campaign WHERE title = '[QA] 채널 지표 499/500 경계 검수');

UPDATE collaboration_campaign
SET start_at = NOW() - INTERVAL '1 day', end_at = NOW() + INTERVAL '7 days', open = TRUE, updated_at = NOW()
WHERE title = '[QA] 채널 지표 499/500 경계 검수';

INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json, agreements_json,
    version, created_at, updated_at
)
SELECT campaign.id, '["VISIT"]', '[]', '[]', 1, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title = '[QA] 채널 지표 499/500 경계 검수'
ON CONFLICT (campaign_id) DO NOTHING;

INSERT INTO campaign_eligibility_requirement (
    campaign_id, channel_type, metric_type, minimum_value, measurement_period, created_at, updated_at
)
SELECT campaign.id, 'INSTAGRAM', 'FOLLOWER_COUNT', 500, NULL, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title = '[QA] 채널 지표 499/500 경계 검수'
ON CONFLICT (campaign_id) DO UPDATE SET
    channel_type = EXCLUDED.channel_type,
    metric_type = EXCLUDED.metric_type,
    minimum_value = EXCLUDED.minimum_value,
    measurement_period = EXCLUDED.measurement_period,
    updated_at = NOW();

INSERT INTO member_channel_metric (
    member_id, channel_type, metric_type, metric_value, measured_at,
    verification_status, created_at, updated_at
)
SELECT member.id, 'INSTAGRAM', 'FOLLOWER_COUNT', fixture.metric_value, NOW(),
       'VERIFIED', NOW(), NOW()
FROM (VALUES
    ('qa-metric-499-integration@movrit.local', 499::bigint),
    ('qa-metric-500-integration@movrit.local', 500::bigint)
) AS fixture(email, metric_value)
JOIN member ON member.email = fixture.email
ON CONFLICT (member_id, channel_type, metric_type) DO UPDATE SET
    metric_value = EXCLUDED.metric_value,
    measured_at = EXCLUDED.measured_at,
    verification_status = EXCLUDED.verification_status,
    updated_at = NOW();

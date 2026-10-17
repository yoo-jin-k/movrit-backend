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

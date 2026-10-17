-- 기존 진행 캠페인의 모집 정원을 바꾸지 않고 선정·미선정을 검수하는 전용 fixture입니다.
INSERT INTO preorder (
    product_id, title, description, preorder_price, currency, target_quantity, max_quantity,
    ordered_quantity, start_at, end_at, status, version, created_at, updated_at,
    min_order_quantity, max_order_quantity_per_member
)
SELECT source.product_id, '[QA] 브랜드 A 선정 전용 프리오더', '선정·미선정 상태 전이 전용',
       source.preorder_price, source.currency, 10, 20, 0,
       NOW() - INTERVAL '2 days', NOW() + INTERVAL '20 days', 'OPEN', 0, NOW(), NOW(), 1, 1
FROM preorder source
WHERE source.id = 15
  AND NOT EXISTS (SELECT 1 FROM preorder WHERE title = '[QA] 브랜드 A 선정 전용 프리오더');

INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count, recruitment_count,
    reward_description, start_at, end_at, open, created_at, updated_at, schedule_version
)
SELECT preorder.id, '[QA] 브랜드 A 선정 전용 캠페인', '선정·미선정 검수 전용',
       'QA', 0, 2, 'QA', NOW() - INTERVAL '2 days', NOW() + INTERVAL '10 days',
       TRUE, NOW(), NOW(), 1
FROM preorder
WHERE preorder.title = '[QA] 브랜드 A 선정 전용 프리오더'
  AND NOT EXISTS (SELECT 1 FROM collaboration_campaign WHERE title = '[QA] 브랜드 A 선정 전용 캠페인');

INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json, agreements_json,
    version, created_at, updated_at
)
SELECT campaign.id, '["DELIVERY"]', '[]', '[]', 1, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title = '[QA] 브랜드 A 선정 전용 캠페인'
ON CONFLICT (campaign_id) DO NOTHING;

INSERT INTO campaign_application (
    campaign_id, creator_id, fulfillment_type, status, message,
    recipient_name, recipient_phone, postal_code, address1, address2,
    answers_json, agreement_acceptances_json, form_version,
    eligibility_requirement_snapshot_json, eligibility_requirement_version,
    identity_verification_configured, identity_verified, email_verified, channel_ready,
    submitted_at, responded_at, cancelled_at, cancellation_reason,
    experience_confirmed_at, created_at, updated_at
)
SELECT campaign.id, fixture.creator_id, 'DELIVERY', 'SUBMITTED', fixture.message,
       fixture.recipient_name, fixture.recipient_phone, fixture.postal_code, fixture.address1, NULL,
       '[]', '[]', 1, NULL, NULL, FALSE, FALSE, TRUE, TRUE,
       NOW() - INTERVAL '1 day', NULL, NULL, NULL, NULL, NOW() - INTERVAL '1 day', NOW()
FROM collaboration_campaign campaign
CROSS JOIN (VALUES
    (5::bigint, '[QA] 정상 선정 전용', '선정 전 비공개 C', '010-3000-3000', '06238', '서울특별시 강남구 QA로 3'),
    (6::bigint, '[QA] 정상 미선정 전용', '선정 전 비공개 D', '010-4000-4000', '06239', '서울특별시 강남구 QA로 4')
) AS fixture(creator_id, message, recipient_name, recipient_phone, postal_code, address1)
WHERE campaign.title = '[QA] 브랜드 A 선정 전용 캠페인'
  AND NOT EXISTS (
      SELECT 1 FROM campaign_application existing
      WHERE existing.campaign_id = campaign.id AND existing.creator_id = fixture.creator_id
  );

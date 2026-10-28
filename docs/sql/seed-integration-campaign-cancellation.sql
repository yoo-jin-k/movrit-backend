-- user_17의 신청 취소 화면을 상태별로 검수하는 독립 fixture입니다.
-- 기존 신청과 알림은 수정하지 않고 새 신청·알림만 추가합니다.

INSERT INTO preorder (
    product_id, title, description, preorder_price, currency,
    target_quantity, max_quantity, ordered_quantity, start_at, end_at,
    status, version, created_at, updated_at, min_order_quantity,
    max_order_quantity_per_member
)
SELECT product.id, fixture.title, '신청 취소 상태 검수용 연결 데이터',
       9000, 'KRW', 10, 100, 0,
       NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days',
       'OPEN', 0, NOW(), NOW(), 1, 1
FROM product
JOIN brand ON brand.id = product.brand_id
CROSS JOIN (VALUES
    ('[QA-CANCEL] 접수 취소 연결 프리오더'),
    ('[QA-CANCEL] 선정 취소 연결 프리오더'),
    ('[QA-CANCEL] 진행 취소 연결 프리오더')
) AS fixture(title)
WHERE product.name = '[QA-WORKSPACE] 콘텐츠 제출 검수 상품'
  AND brand.name = 'QA WORKSPACE'
  AND NOT EXISTS (SELECT 1 FROM preorder existing WHERE existing.title = fixture.title);

INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count,
    recruitment_count, selection_mode, reward_description, start_at, end_at, open,
    selection_announced_at, schedule_version, created_at, updated_at
)
SELECT preorder.id, fixture.campaign_title, '신청 취소 상태 검수용 합성 캠페인',
       'QA', 0, 3, 'BRAND_DIRECT', '로컬 취소 검수',
       NOW() - INTERVAL '2 days', NOW() + INTERVAL '7 days', TRUE,
       NOW() + INTERVAL '8 days', 1, NOW(), NOW()
FROM preorder
JOIN (VALUES
    ('[QA-CANCEL] 접수 취소 연결 프리오더', '[QA-CANCEL] 접수 상태 취소'),
    ('[QA-CANCEL] 선정 취소 연결 프리오더', '[QA-CANCEL] 선정 상태 취소'),
    ('[QA-CANCEL] 진행 취소 연결 프리오더', '[QA-CANCEL] 진행 상태 취소')
) AS fixture(preorder_title, campaign_title) ON fixture.preorder_title = preorder.title
WHERE NOT EXISTS (
    SELECT 1 FROM collaboration_campaign existing WHERE existing.preorder_id = preorder.id
);

INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json,
    agreements_json, version, created_at, updated_at
)
SELECT campaign.id, '["DELIVERY"]', '[]', '[]', 1, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title LIKE '[QA-CANCEL]%'
ON CONFLICT (campaign_id) DO NOTHING;

INSERT INTO campaign_application (
    campaign_id, creator_id, fulfillment_type, status, message,
    recipient_name, recipient_phone, postal_code, address1, address2,
    answers_json, agreement_acceptances_json, form_version,
    identity_verification_configured, identity_verified, email_verified,
    channel_ready, submitted_at, responded_at, created_at, updated_at
)
SELECT campaign.id, source.creator_id, 'DELIVERY', fixture.status,
       '사용자가 직접 취소 동작을 검수하는 합성 신청',
       'QA 신청자', '010-0000-0117', '00000', '서울특별시 테스트구 취소로 17', 'QA',
       '[]', '[]', 1, FALSE, FALSE, TRUE, TRUE,
       NOW() - INTERVAL '1 day',
       CASE WHEN fixture.status = 'SUBMITTED' THEN NULL ELSE NOW() - INTERVAL '12 hours' END,
       NOW(), NOW()
FROM collaboration_campaign campaign
JOIN (VALUES
    ('[QA-CANCEL] 접수 상태 취소', 'SUBMITTED'),
    ('[QA-CANCEL] 선정 상태 취소', 'SELECTED'),
    ('[QA-CANCEL] 진행 상태 취소', 'ACTIVE')
) AS fixture(campaign_title, status) ON fixture.campaign_title = campaign.title
CROSS JOIN (
    SELECT application.creator_id
    FROM campaign_application application
    WHERE application.id = 24
    UNION ALL
    SELECT creator.id
    FROM influencer_profile creator
    JOIN member member ON member.id = creator.member_id
    WHERE member.email = 'qa-workspace-user-integration@movrit.local'
) source
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_application existing
    WHERE existing.campaign_id = campaign.id AND existing.creator_id = source.creator_id
);

INSERT INTO notification (
    member_id, type, title, message, target_type, target_id, is_read, created_at
)
SELECT creator.member_id, 'CAMPAIGN_APPLICATION',
       '[QA-CANCEL] ' || application.status || ' 신청 확인',
       '신청 취소 화면 연결을 검수해 주세요.',
       'CAMPAIGN_APPLICATION', application.id, FALSE, NOW()
FROM campaign_application application
JOIN collaboration_campaign campaign ON campaign.id = application.campaign_id
JOIN influencer_profile creator ON creator.id = application.creator_id
WHERE campaign.title LIKE '[QA-CANCEL]%'
  AND NOT EXISTS (
      SELECT 1 FROM notification existing
      WHERE existing.target_type = 'CAMPAIGN_APPLICATION'
        AND existing.target_id = application.id
        AND existing.title LIKE '[QA-CANCEL]%'
  );

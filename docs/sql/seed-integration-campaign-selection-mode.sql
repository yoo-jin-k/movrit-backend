-- 두 선정 방식을 실제 API로 검수하기 위한 독립 fixture입니다.
-- 비밀번호 평문은 저장하지 않고 기존 혼합 QA 계정의 해시만 재사용합니다.

INSERT INTO member (
    email, password_hash, name, nickname, role, status,
    email_verified_at, created_at, updated_at
)
SELECT 'qa-selection-operator-integration@movrit.local', source.password_hash,
       '선정 운영자 QA', 'qa_select_admin', 'ADMIN', 'ACTIVE',
       NOW(), NOW(), NOW()
FROM member source
WHERE source.email = 'qa-mixed-user-insta-low-integration@movrit.local'
  AND NOT EXISTS (
      SELECT 1 FROM member
      WHERE email = 'qa-selection-operator-integration@movrit.local'
  );

INSERT INTO preorder (
    product_id, title, description, preorder_price, currency,
    target_quantity, max_quantity, ordered_quantity, start_at, end_at,
    status, version, created_at, updated_at, min_order_quantity,
    max_order_quantity_per_member
)
SELECT product.id, fixture.title, fixture.description, 9000, 'KRW',
       10, 100, 0, NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days',
       'OPEN', 0, NOW(), NOW(), 1, 1
FROM product
JOIN brand ON brand.id = product.brand_id
CROSS JOIN (VALUES
    ('[QA-SELECTION] 브랜드 직접 선정 연결 프리오더', '브랜드 직접 선정 권한 검수용'),
    ('[QA-SELECTION] 운영자 선정 연결 프리오더', '운영자 선정 권한 검수용')
) AS fixture(title, description)
WHERE product.name = '[QA-WORKSPACE] 콘텐츠 제출 검수 상품'
  AND brand.name = 'QA WORKSPACE'
  AND NOT EXISTS (
      SELECT 1 FROM preorder existing WHERE existing.title = fixture.title
  );

INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count,
    recruitment_count, selection_mode, reward_description, start_at, end_at, open,
    selection_announced_at, schedule_version, created_at, updated_at
)
SELECT preorder.id, fixture.campaign_title, fixture.description, 'QA', 0,
       2, fixture.selection_mode, '로컬 선정 권한 검수',
       NOW() - INTERVAL '2 days', NOW() + INTERVAL '7 days', TRUE,
       NOW() + INTERVAL '8 days', 1, NOW(), NOW()
FROM preorder
JOIN (VALUES
    ('[QA-SELECTION] 브랜드 직접 선정 연결 프리오더', '[QA-SELECTION] 브랜드 직접 선정',
     '소유 브랜드만 선정할 수 있는 합성 캠페인', 'BRAND_DIRECT'),
    ('[QA-SELECTION] 운영자 선정 연결 프리오더', '[QA-SELECTION] 운영자 선정',
     '운영자만 선정할 수 있는 합성 캠페인', 'OPERATOR')
) AS fixture(preorder_title, campaign_title, description, selection_mode)
  ON fixture.preorder_title = preorder.title
WHERE NOT EXISTS (
    SELECT 1 FROM collaboration_campaign existing
    WHERE existing.preorder_id = preorder.id
);

INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json,
    agreements_json, version, created_at, updated_at
)
SELECT campaign.id, '["DELIVERY"]', '[]', '[]', 1, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title IN (
    '[QA-SELECTION] 브랜드 직접 선정',
    '[QA-SELECTION] 운영자 선정'
)
ON CONFLICT (campaign_id) DO NOTHING;

INSERT INTO campaign_application (
    campaign_id, creator_id, fulfillment_type, status, message,
    recipient_name, recipient_phone, postal_code, address1, address2,
    answers_json, agreement_acceptances_json, form_version,
    identity_verification_configured, identity_verified, email_verified,
    channel_ready, submitted_at, created_at, updated_at
)
SELECT campaign.id, source.creator_id, 'DELIVERY', 'SUBMITTED',
       '선정 주체 권한 검수용 합성 신청',
       'QA 신청자', '010-0000-0117', '00000', '서울특별시 테스트구 선정로 17', 'QA',
       '[]', '[]', 1, FALSE, FALSE, TRUE, TRUE, NOW(), NOW(), NOW()
FROM collaboration_campaign campaign
CROSS JOIN (
    SELECT application.creator_id
    FROM campaign_application application
    WHERE application.id = 24
) source
WHERE campaign.title IN (
    '[QA-SELECTION] 브랜드 직접 선정',
    '[QA-SELECTION] 운영자 선정'
)
  AND NOT EXISTS (
      SELECT 1 FROM campaign_application existing
      WHERE existing.campaign_id = campaign.id
        AND existing.creator_id = source.creator_id
  );

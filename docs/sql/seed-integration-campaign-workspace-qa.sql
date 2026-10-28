-- 내 캠페인 콘텐츠 제출·검수 흐름을 다른 데모 데이터와 분리해 확인하는 통합 DB 전용 fixture입니다.
-- 평문 비밀번호는 저장하지 않고 기존 로컬 QA 계정의 해시만 재사용합니다.

INSERT INTO member (
    email, password_hash, name, nickname, role, status,
    email_verified_at, created_at, updated_at
)
SELECT fixture.email, source.password_hash, fixture.name, fixture.nickname,
       'USER', 'ACTIVE', NOW(), NOW(), NOW()
FROM member source
CROSS JOIN (VALUES
    ('qa-workspace-user-integration@movrit.local', '작업공간 참여자', 'qa_workspace_user'),
    ('qa-workspace-owner-integration@movrit.local', '작업공간 브랜드', 'qa_workspace_owner')
) AS fixture(email, name, nickname)
WHERE source.email = 'qa-mixed-user-insta-low-integration@movrit.local'
ON CONFLICT (email) DO NOTHING;

INSERT INTO seller_profile (
    member_id, business_name, brand_name, business_number, representative_name,
    status, created_at, updated_at
)
SELECT member.id, 'QA 작업공간 사업자', 'QA WORKSPACE', 'QA-WORKSPACE-OWNER',
       'QA 담당자', 'APPROVED', NOW(), NOW()
FROM member
WHERE member.email = 'qa-workspace-owner-integration@movrit.local'
ON CONFLICT (member_id) DO NOTHING;

INSERT INTO brand (seller_id, name, description, status, created_at, updated_at)
SELECT seller.id, 'QA WORKSPACE', '내 캠페인 콘텐츠 흐름 검수 전용 합성 브랜드',
       'ACTIVE', NOW(), NOW()
FROM seller_profile seller
WHERE seller.business_number = 'QA-WORKSPACE-OWNER'
  AND NOT EXISTS (
      SELECT 1 FROM brand WHERE brand.seller_id = seller.id AND brand.name = 'QA WORKSPACE'
  );

INSERT INTO product (
    brand_id, name, description, category, normal_price, status,
    thumbnail_url, created_at, updated_at
)
SELECT brand.id, '[QA-WORKSPACE] 콘텐츠 제출 검수 상품',
       '주문·결제 없이 캠페인 작업공간만 검수하는 합성 상품',
       '생활', 10000, 'ACTIVE', '/assets/discovery/banners/living.png', NOW(), NOW()
FROM brand
WHERE brand.name = 'QA WORKSPACE'
  AND NOT EXISTS (
      SELECT 1 FROM product
      WHERE product.brand_id = brand.id
        AND product.name = '[QA-WORKSPACE] 콘텐츠 제출 검수 상품'
  );

INSERT INTO product_variant (
    product_id, sku_code, option_name_1, option_value_1, additional_price,
    on_hand_quantity, reserved_quantity, safety_stock_quantity, sold_quantity,
    active, version, created_at, updated_at
)
SELECT product.id, 'QA-WORKSPACE-CONTENT-01', '구성', '검수용', 0,
       100, 0, 0, 0, TRUE, 0, NOW(), NOW()
FROM product
WHERE product.name = '[QA-WORKSPACE] 콘텐츠 제출 검수 상품'
ON CONFLICT (sku_code) DO NOTHING;

INSERT INTO preorder (
    product_id, title, description, preorder_price, currency,
    target_quantity, max_quantity, ordered_quantity, start_at, end_at,
    status, version, created_at, updated_at, min_order_quantity,
    max_order_quantity_per_member
)
SELECT product.id, '[QA-WORKSPACE] 콘텐츠 제출 검수 프리오더',
       '실제 주문을 만들지 않는 작업공간 검수용 연결 데이터',
       9000, 'KRW', 10, 100, 0, NOW() - INTERVAL '1 day', NOW() + INTERVAL '30 days',
       'OPEN', 0, NOW(), NOW(), 1, 1
FROM product
WHERE product.name = '[QA-WORKSPACE] 콘텐츠 제출 검수 상품'
  AND NOT EXISTS (SELECT 1 FROM preorder WHERE preorder.product_id = product.id);

INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count,
    recruitment_count, reward_description, start_at, end_at, open,
    mission, channel_guide, schedule_guide, caution, submission_requirement,
    selection_announced_at, experience_start_at, experience_end_at,
    content_submission_start_at, content_submission_end_at, results_announced_at,
    schedule_version, representative_image_url, contact_name, contact_phone,
    created_at, updated_at
)
SELECT preorder.id, '[QA-WORKSPACE] 채널 링크 제출 검수',
       '내 캠페인 화면과 브랜드 검수 화면을 연결하는 합성 캠페인',
       '생활', 0, 5, '검수용 가상 상품 제공',
       NOW() - INTERVAL '20 days', NOW() - INTERVAL '10 days', FALSE,
       '채널별 게시물 URL을 제출해 주세요.',
       'Instagram, Blog, YouTube 중 실제 검수할 채널을 선택해 주세요.',
       '현재 제출 기간이 열려 있습니다.',
       '외부 사이트를 자동 조회하지 않는 로컬 검수 데이터입니다.',
       '공개 게시물 URL을 제출하고 수정 요청이 오면 전체 링크를 다시 제출해 주세요.',
       NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days',
       NOW() - INTERVAL '1 day', NOW() + INTERVAL '14 days', NOW() + INTERVAL '16 days',
       1, '/assets/discovery/banners/living.png', 'QA 작업공간 담당자', '02-0000-0099',
       NOW(), NOW()
FROM preorder
WHERE preorder.title = '[QA-WORKSPACE] 콘텐츠 제출 검수 프리오더'
  AND NOT EXISTS (
      SELECT 1 FROM collaboration_campaign
      WHERE collaboration_campaign.preorder_id = preorder.id
  );

INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json,
    agreements_json, version, created_at, updated_at
)
SELECT campaign.id, '["DELIVERY"]', '[]', '[]', 1, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title = '[QA-WORKSPACE] 채널 링크 제출 검수'
ON CONFLICT (campaign_id) DO NOTHING;

INSERT INTO influencer_profile (
    member_id, nickname, bio, primary_category, status,
    sns_url, follower_count, created_at, updated_at
)
SELECT member.id, '작업공간 참여자', '콘텐츠 제출 검수용 채널', '생활',
       'ACTIVE', 'https://instagram.com/movrit_workspace_qa', 1000, NOW(), NOW()
FROM member
WHERE member.email = 'qa-workspace-user-integration@movrit.local'
ON CONFLICT (member_id) DO NOTHING;

INSERT INTO campaign_application (
    campaign_id, creator_id, fulfillment_type, status, message,
    recipient_name, recipient_phone, postal_code, address1, address2,
    answers_json, agreement_acceptances_json, form_version,
    identity_verification_configured, identity_verified, email_verified,
    channel_ready, submitted_at, responded_at, experience_confirmed_at,
    created_at, updated_at
)
SELECT campaign.id, creator.id, 'DELIVERY', 'ACTIVE',
       '내 캠페인 콘텐츠 제출 검수용 신청입니다.',
       'QA 참여자', '010-0000-0099', '00000', '서울특별시 테스트구 검수로 1', 'QA',
       '[]', '[]', 1, FALSE, FALSE, TRUE, TRUE,
       NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days',
       NOW(), NOW()
FROM collaboration_campaign campaign
JOIN influencer_profile creator ON creator.member_id = (
    SELECT id FROM member WHERE email = 'qa-workspace-user-integration@movrit.local'
)
WHERE campaign.title = '[QA-WORKSPACE] 채널 링크 제출 검수'
  AND NOT EXISTS (
      SELECT 1 FROM campaign_application application
      WHERE application.campaign_id = campaign.id
        AND application.creator_id = creator.id
  );

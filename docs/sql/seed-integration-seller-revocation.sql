-- 업체 권한 회수 전후의 민감정보·쓰기 차단을 기존 A/B와 분리해 검수합니다.
INSERT INTO member (email, password_hash, name, role, status, email_verified_at, created_at, updated_at)
SELECT 'qa-revocable-seller-integration@movrit.local', source.password_hash,
       'QA 권한회수 판매자', 'USER', 'ACTIVE', NOW(), NOW(), NOW()
FROM member source
WHERE source.email = 'qa-browser-integration@movrit.local'
ON CONFLICT (email) DO NOTHING;

INSERT INTO seller_profile (
    member_id, business_name, brand_name, business_number, representative_name,
    status, created_at, updated_at
)
SELECT member.id, 'QA 권한회수 사업자', 'QA REVOCABLE', 'QA-REVOCABLE-SELLER',
       'QA 회수', 'APPROVED', NOW(), NOW()
FROM member
WHERE member.email = 'qa-revocable-seller-integration@movrit.local'
ON CONFLICT (member_id) DO UPDATE SET status = 'APPROVED', updated_at = NOW();

INSERT INTO brand (seller_id, name, description, status, created_at, updated_at)
SELECT seller.id, 'QA REVOCABLE', '권한 회수 검수 전용 브랜드', 'ACTIVE', NOW(), NOW()
FROM seller_profile seller
WHERE seller.business_number = 'QA-REVOCABLE-SELLER'
  AND NOT EXISTS (SELECT 1 FROM brand WHERE brand.seller_id = seller.id);

INSERT INTO product (brand_id, name, description, category, normal_price, status, created_at, updated_at)
SELECT brand.id, 'QA 권한회수 검수 상품', '권한 회수 검수 전용', 'QA', 10000, 'ACTIVE', NOW(), NOW()
FROM brand
WHERE brand.name = 'QA REVOCABLE'
  AND NOT EXISTS (SELECT 1 FROM product WHERE product.brand_id = brand.id);

INSERT INTO preorder (
    product_id, title, description, preorder_price, currency, target_quantity, max_quantity,
    ordered_quantity, start_at, end_at, status, version, created_at, updated_at,
    min_order_quantity, max_order_quantity_per_member
)
SELECT product.id, '[QA] 업체 권한회수 프리오더', '권한 회수 검수 전용',
       9000, 'KRW', 10, 20, 0, NOW() - INTERVAL '1 day', NOW() + INTERVAL '20 days',
       'OPEN', 0, NOW(), NOW(), 1, 1
FROM product
WHERE product.name = 'QA 권한회수 검수 상품'
  AND NOT EXISTS (SELECT 1 FROM preorder WHERE preorder.product_id = product.id);

INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count, recruitment_count,
    reward_description, start_at, end_at, open, created_at, updated_at, schedule_version
)
SELECT preorder.id, '[QA] 업체 권한회수 캠페인', '권한 회수 민감정보 검수 전용',
       'QA', 0, 2, 'QA', NOW() - INTERVAL '1 day', NOW() + INTERVAL '10 days',
       TRUE, NOW(), NOW(), 1
FROM preorder
WHERE preorder.title = '[QA] 업체 권한회수 프리오더'
  AND NOT EXISTS (SELECT 1 FROM collaboration_campaign WHERE title = '[QA] 업체 권한회수 캠페인');

INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json, agreements_json,
    version, created_at, updated_at
)
SELECT campaign.id, '["DELIVERY"]', '[]', '[]', 1, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title = '[QA] 업체 권한회수 캠페인'
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
SELECT campaign.id, 5, 'DELIVERY', 'CONTENT_SUBMITTED', '[QA] 권한회수 민감 신청',
       '권한회수 비공개 수령인', '010-5000-5000', '06240', '서울특별시 강남구 QA로 5', NULL,
       '[]', '[]', 1, NULL, NULL, FALSE, FALSE, TRUE, TRUE,
       NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', NULL, NULL,
       NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NOW()
FROM collaboration_campaign campaign
WHERE campaign.title = '[QA] 업체 권한회수 캠페인'
  AND NOT EXISTS (SELECT 1 FROM campaign_application WHERE campaign_id = campaign.id);

INSERT INTO campaign_content_submission (
    application_id, content_url, note, status, revision_count, version,
    review_comment, submitted_at, reviewed_at, created_at, updated_at
)
SELECT application.id, 'https://example.com/movrit/revocation-sensitive-content',
       '권한 회수 후 비노출 검수', 'SUBMITTED', 0, 0, NULL,
       NOW() - INTERVAL '1 hour', NULL, NOW() - INTERVAL '1 hour', NOW()
FROM campaign_application application
WHERE application.message = '[QA] 권한회수 민감 신청'
ON CONFLICT (application_id) DO NOTHING;

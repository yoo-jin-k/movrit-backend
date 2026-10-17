-- 브랜드 A의 콘텐츠 검토 UI를 기존 캠페인·신청과 분리해 검수하는 통합 DB 전용 fixture입니다.
INSERT INTO preorder (
    product_id, title, description, preorder_price, currency, target_quantity, max_quantity,
    ordered_quantity, start_at, end_at, status, version, created_at, updated_at,
    min_order_quantity, max_order_quantity_per_member
)
SELECT source.product_id, '[QA] 브랜드 A 콘텐츠 검토 프리오더', '콘텐츠 검토 상태 전이 전용',
       source.preorder_price, source.currency, 10, 20, 0,
       NOW() - INTERVAL '2 days', NOW() + INTERVAL '20 days', 'OPEN', 0, NOW(), NOW(), 1, 1
FROM preorder source
WHERE source.id = 15
  AND NOT EXISTS (SELECT 1 FROM preorder WHERE title = '[QA] 브랜드 A 콘텐츠 검토 프리오더');

INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count, recruitment_count,
    reward_description, start_at, end_at, open, created_at, updated_at, schedule_version
)
SELECT preorder.id, '[QA] 브랜드 A 콘텐츠 검토 캠페인', '제출·수정요청·승인 검수 전용',
       'QA', 0, 3, 'QA', NOW() - INTERVAL '2 days', NOW() + INTERVAL '10 days',
       TRUE, NOW(), NOW(), 1
FROM preorder
WHERE preorder.title = '[QA] 브랜드 A 콘텐츠 검토 프리오더'
  AND NOT EXISTS (SELECT 1 FROM collaboration_campaign WHERE title = '[QA] 브랜드 A 콘텐츠 검토 캠페인');

INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json, agreements_json,
    version, created_at, updated_at
)
SELECT campaign.id, '["VISIT"]', '[]', '[]', 1, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title = '[QA] 브랜드 A 콘텐츠 검토 캠페인'
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
SELECT campaign.id, source.creator_id, 'VISIT', 'CONTENT_SUBMITTED', '콘텐츠 검토 QA 신청',
       NULL, NULL, NULL, NULL, NULL, '[]', '[]', 1, NULL, NULL,
       FALSE, FALSE, TRUE, TRUE, NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days',
       NULL, NULL, NOW() - INTERVAL '1 day', NOW() - INTERVAL '3 days', NOW()
FROM collaboration_campaign campaign
JOIN campaign_application source ON source.id = 11
WHERE campaign.title = '[QA] 브랜드 A 콘텐츠 검토 캠페인'
  AND NOT EXISTS (SELECT 1 FROM campaign_application WHERE campaign_id = campaign.id);

INSERT INTO campaign_content_submission (
    application_id, content_url, note, status, revision_count, version,
    review_comment, submitted_at, reviewed_at, created_at, updated_at
)
SELECT application.id, 'https://example.com/movrit/seller-dashboard-content-qa',
       '업체 대시보드 검수 전용 콘텐츠', 'SUBMITTED', 0, 0,
       NULL, NOW() - INTERVAL '1 hour', NULL, NOW() - INTERVAL '1 hour', NOW()
FROM campaign_application application
JOIN collaboration_campaign campaign ON campaign.id = application.campaign_id
WHERE campaign.title = '[QA] 브랜드 A 콘텐츠 검토 캠페인'
ON CONFLICT (application_id) DO NOTHING;

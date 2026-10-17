-- 업체 대시보드의 승인, 충돌 복구, 선정/미선정을 서로 독립적으로 검수하는 데이터입니다.
INSERT INTO campaign_application (
    campaign_id, creator_id, fulfillment_type, status, message,
    recipient_name, recipient_phone, postal_code, address1, address2,
    answers_json, agreement_acceptances_json, form_version,
    eligibility_requirement_snapshot_json, eligibility_requirement_version,
    identity_verification_configured, identity_verified, email_verified, channel_ready,
    submitted_at, responded_at, cancelled_at, cancellation_reason,
    experience_confirmed_at, created_at, updated_at
)
SELECT campaign.id, fixture.creator_id, fixture.fulfillment_type, fixture.status, fixture.message,
       fixture.recipient_name, fixture.recipient_phone, fixture.postal_code, fixture.address1, NULL,
       '[]', '[]', 1, NULL, NULL, FALSE, FALSE, TRUE, TRUE,
       NOW() - INTERVAL '2 days', NULL, NULL, NULL,
       CASE WHEN fixture.status = 'CONTENT_SUBMITTED' THEN NOW() - INTERVAL '1 day' END,
       NOW() - INTERVAL '2 days', NOW()
FROM collaboration_campaign campaign
CROSS JOIN (VALUES
    (5::bigint, 'VISIT', 'CONTENT_SUBMITTED', '[QA] 업체 정상 승인 콘텐츠', NULL, NULL, NULL, NULL),
    (6::bigint, 'VISIT', 'CONTENT_SUBMITTED', '[QA] 콘텐츠 409 복구', NULL, NULL, NULL, NULL),
    (7::bigint, 'DELIVERY', 'SUBMITTED', '[QA] 선정 후보', '선정전 비공개 A', '010-1000-1000', '06236', '서울특별시 강남구 QA로 1'),
    (8::bigint, 'DELIVERY', 'SUBMITTED', '[QA] 미선정 후보', '선정전 비공개 B', '010-2000-2000', '06237', '서울특별시 강남구 QA로 2')
) AS fixture(creator_id, fulfillment_type, status, message, recipient_name, recipient_phone, postal_code, address1)
WHERE campaign.title = '[QA] 브랜드 A 콘텐츠 검토 캠페인'
  AND NOT EXISTS (
      SELECT 1 FROM campaign_application existing
      WHERE existing.campaign_id = campaign.id AND existing.creator_id = fixture.creator_id
  );

INSERT INTO campaign_content_submission (
    application_id, content_url, note, status, revision_count, version,
    review_comment, submitted_at, reviewed_at, created_at, updated_at
)
SELECT application.id,
       CASE application.message
           WHEN '[QA] 업체 정상 승인 콘텐츠' THEN 'https://example.com/movrit/brand-approve-qa'
           ELSE 'https://example.com/movrit/content-conflict-qa'
       END,
       application.message, 'SUBMITTED', 0, 0, NULL,
       NOW() - INTERVAL '30 minutes', NULL, NOW() - INTERVAL '30 minutes', NOW()
FROM campaign_application application
WHERE application.message IN ('[QA] 업체 정상 승인 콘텐츠', '[QA] 콘텐츠 409 복구')
ON CONFLICT (application_id) DO NOTHING;

-- 비승인 판매자 대시보드 접근 검수용 계정입니다. 평문 비밀번호는 저장하지 않습니다.
INSERT INTO member (email, password_hash, name, role, status, email_verified_at, created_at, updated_at)
SELECT 'qa-rejected-seller-integration@movrit.local', source.password_hash,
       'QA 비승인 판매자', 'USER', 'ACTIVE', NOW(), NOW(), NOW()
FROM member source
WHERE source.email = 'qa-browser-integration@movrit.local'
ON CONFLICT (email) DO NOTHING;

INSERT INTO seller_profile (
    member_id, business_name, brand_name, business_number, representative_name,
    status, created_at, updated_at
)
SELECT member.id, 'QA 비승인 사업자', 'QA REJECTED', 'QA-REJECTED-SELLER',
       'QA 거절', 'REJECTED', NOW(), NOW()
FROM member
WHERE member.email = 'qa-rejected-seller-integration@movrit.local'
ON CONFLICT (member_id) DO NOTHING;

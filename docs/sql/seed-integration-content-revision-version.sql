-- 수정 요청 응답 버전을 기존 승인·충돌 검수 제출물과 분리해 확인합니다.
INSERT INTO campaign_application (
    campaign_id, creator_id, fulfillment_type, status, message,
    recipient_name, recipient_phone, postal_code, address1, address2,
    answers_json, agreement_acceptances_json, form_version,
    eligibility_requirement_snapshot_json, eligibility_requirement_version,
    identity_verification_configured, identity_verified, email_verified, channel_ready,
    submitted_at, responded_at, cancelled_at, cancellation_reason,
    experience_confirmed_at, created_at, updated_at
)
SELECT campaign.id, 7, 'VISIT', 'CONTENT_SUBMITTED', '[QA] 수정 요청 응답 버전 전용',
       NULL, NULL, NULL, NULL, NULL, '[]', '[]', 1, NULL, NULL,
       FALSE, FALSE, TRUE, TRUE, NOW() - INTERVAL '2 days', NULL, NULL, NULL,
       NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NOW()
FROM collaboration_campaign campaign
WHERE campaign.title = '[QA] 브랜드 A 선정 전용 캠페인'
  AND NOT EXISTS (
      SELECT 1 FROM campaign_application existing
      WHERE existing.campaign_id = campaign.id AND existing.creator_id = 7
  );

INSERT INTO campaign_content_submission (
    application_id, content_url, note, status, revision_count, version,
    review_comment, submitted_at, reviewed_at, created_at, updated_at
)
SELECT application.id, 'https://example.com/movrit/revision-response-version-qa',
       '수정 요청 응답 버전 전용', 'SUBMITTED', 0, 0, NULL,
       NOW() - INTERVAL '10 minutes', NULL, NOW() - INTERVAL '10 minutes', NOW()
FROM campaign_application application
WHERE application.message = '[QA] 수정 요청 응답 버전 전용'
ON CONFLICT (application_id) DO NOTHING;

-- 업체 참여자 포트폴리오의 완료 집계·공개 링크·권한을 검수하는 독립 fixture입니다.
-- 평문 비밀번호는 저장하지 않고 기존 혼합 QA 계정의 해시만 재사용합니다.

INSERT INTO member (
    email, password_hash, name, nickname, role, status,
    email_verified_at, created_at, updated_at
)
SELECT 'qa-portfolio-participant-integration@movrit.local', source.password_hash,
       '포트폴리오 참여자', 'qa_portfolio_user', 'USER', 'ACTIVE', NOW(), NOW(), NOW()
FROM member source
WHERE source.email = 'qa-mixed-user-insta-low-integration@movrit.local'
ON CONFLICT (email) DO NOTHING;

INSERT INTO influencer_profile (
    member_id, nickname, bio, primary_category, sns_url,
    follower_count, status, created_at, updated_at
)
SELECT member.id, '포트폴리오채널', '캠페인 완료 이력 검수용 활동 소개', '생활',
       'https://instagram.com/movrit_portfolio_qa', 2500, 'ACTIVE', NOW(), NOW()
FROM member
WHERE member.email = 'qa-portfolio-participant-integration@movrit.local'
ON CONFLICT (member_id) DO NOTHING;

INSERT INTO member_channel_metric (
    member_id, channel_type, metric_type, metric_value,
    measured_at, verification_status, created_at, updated_at
)
SELECT member.id, 'INSTAGRAM', 'FOLLOWER_COUNT', 2500,
       NOW() - INTERVAL '1 day', 'VERIFIED', NOW(), NOW()
FROM member
WHERE member.email = 'qa-portfolio-participant-integration@movrit.local'
ON CONFLICT (member_id, channel_type, metric_type) DO NOTHING;

INSERT INTO preorder (
    product_id, title, description, preorder_price, currency,
    target_quantity, max_quantity, ordered_quantity, start_at, end_at,
    status, version, created_at, updated_at, min_order_quantity,
    max_order_quantity_per_member
)
SELECT product.id, fixture.title, '포트폴리오 검수용 연결 데이터',
       9000, 'KRW', 10, 100, 0,
       NOW() - INTERVAL '60 days', NOW() + INTERVAL '30 days',
       'OPEN', 0, NOW(), NOW(), 1, 1
FROM product
JOIN brand ON brand.id = product.brand_id
JOIN (VALUES
    ('QA WORKSPACE', '[QA-PORTFOLIO] 현재 후보 연결 프리오더'),
    ('QA WORKSPACE', '[QA-PORTFOLIO] 완료 이력 연결 프리오더'),
    ('QA WORKSPACE', '[QA-PORTFOLIO] 미완료 이력 연결 프리오더'),
    ('QA ALPHA', '[QA-PORTFOLIO] 타 브랜드 완료 연결 프리오더')
) AS fixture(brand_name, title) ON fixture.brand_name = brand.name
WHERE product.id = CASE WHEN brand.name = 'QA WORKSPACE' THEN 29 ELSE 17 END
  AND NOT EXISTS (SELECT 1 FROM preorder existing WHERE existing.title = fixture.title);

INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count,
    recruitment_count, selection_mode, reward_description, start_at, end_at, open,
    selection_announced_at, experience_start_at, experience_end_at,
    content_submission_start_at, content_submission_end_at, results_announced_at,
    schedule_version, created_at, updated_at
)
SELECT preorder.id, fixture.campaign_title, '포트폴리오 API 검수용 합성 캠페인',
       fixture.category, 0, 3, 'BRAND_DIRECT', '로컬 검수',
       NOW() - INTERVAL '50 days', NOW() - INTERVAL '40 days', FALSE,
       NOW() - INTERVAL '39 days', NOW() - INTERVAL '38 days', NOW() - INTERVAL '30 days',
       NOW() - INTERVAL '29 days', NOW() - INTERVAL '20 days', NOW() - INTERVAL '19 days',
       1, NOW(), NOW()
FROM preorder
JOIN (VALUES
    ('[QA-PORTFOLIO] 현재 후보 연결 프리오더', '[QA-PORTFOLIO] 현재 후보', '생활'),
    ('[QA-PORTFOLIO] 완료 이력 연결 프리오더', '[QA-PORTFOLIO] 완료 이력', '생활'),
    ('[QA-PORTFOLIO] 미완료 이력 연결 프리오더', '[QA-PORTFOLIO] 미완료 이력', '뷰티'),
    ('[QA-PORTFOLIO] 타 브랜드 완료 연결 프리오더', '[QA-PORTFOLIO] 타 브랜드 완료', '뷰티')
) AS fixture(preorder_title, campaign_title, category) ON fixture.preorder_title = preorder.title
WHERE NOT EXISTS (
    SELECT 1 FROM collaboration_campaign existing WHERE existing.preorder_id = preorder.id
);

INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json,
    agreements_json, version, created_at, updated_at
)
SELECT campaign.id, '["DELIVERY"]',
       CASE WHEN campaign.title = '[QA-PORTFOLIO] 현재 후보'
            THEN '[{"id":"motivation","label":"지원 동기를 알려주세요","required":true,"type":"TEXTAREA","options":[],"maxLength":500}]'
            ELSE '[]' END,
       '[]', 1, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title LIKE '[QA-PORTFOLIO]%'
ON CONFLICT (campaign_id) DO NOTHING;

INSERT INTO campaign_eligibility_requirement (
    campaign_id, channel_type, metric_type, minimum_value,
    version, created_at, updated_at
)
SELECT campaign.id, 'INSTAGRAM', 'FOLLOWER_COUNT', 1000, 0, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.title = '[QA-PORTFOLIO] 현재 후보'
ON CONFLICT (campaign_id) DO NOTHING;

INSERT INTO campaign_application (
    campaign_id, creator_id, fulfillment_type, status, message,
    recipient_name, recipient_phone, postal_code, address1, address2,
    answers_json, agreement_acceptances_json, form_version,
    eligibility_requirement_snapshot_json, eligibility_requirement_version,
    identity_verification_configured, identity_verified, email_verified,
    channel_ready, submitted_at, responded_at, experience_confirmed_at,
    created_at, updated_at
)
SELECT campaign.id, creator.id, 'DELIVERY', fixture.status,
       '포트폴리오 상세 검수용 신청',
       '비공개 수령인', '010-0000-0999', '00000', '비공개 주소', '비공개 상세',
       CASE
           WHEN campaign.title = '[QA-PORTFOLIO] 현재 후보'
               THEN '[{"questionId":"motivation","value":"제품을 꾸준히 사용해 진솔하게 소개하겠습니다."}]'
           WHEN campaign.title = '[QA-PORTFOLIO] 타 브랜드 완료'
               THEN '[{"questionId":"qa_private_portfolio_answer","value":"QA_PORTFOLIO_OTHER_BRAND_38_ONLY"}]'
           ELSE '[]'
       END,
       '[]', 1,
       CASE WHEN campaign.title = '[QA-PORTFOLIO] 현재 후보'
            THEN json_build_object(
                'id', requirement.id, 'campaignId', campaign.id,
                'channelType', requirement.channel_type,
                'metricType', requirement.metric_type,
                'minimumValue', requirement.minimum_value,
                'measurementPeriod', requirement.measurement_period,
                'version', requirement.version
            )::text ELSE NULL END,
       CASE WHEN campaign.title = '[QA-PORTFOLIO] 현재 후보' THEN 0 ELSE NULL END,
       FALSE, FALSE, TRUE, TRUE,
       NOW() - INTERVAL '45 days',
       CASE WHEN fixture.status = 'SUBMITTED' THEN NULL ELSE NOW() - INTERVAL '44 days' END,
       CASE WHEN fixture.status = 'SUBMITTED' THEN NULL ELSE NOW() - INTERVAL '35 days' END,
       NOW(), NOW()
FROM collaboration_campaign campaign
JOIN (VALUES
    ('[QA-PORTFOLIO] 현재 후보', 'SUBMITTED'),
    ('[QA-PORTFOLIO] 완료 이력', 'COMPLETED'),
    ('[QA-PORTFOLIO] 미완료 이력', 'CONTENT_SUBMITTED'),
    ('[QA-PORTFOLIO] 타 브랜드 완료', 'COMPLETED')
) AS fixture(campaign_title, status) ON fixture.campaign_title = campaign.title
JOIN influencer_profile creator ON creator.member_id = (
    SELECT member.id FROM member WHERE member.email = 'qa-portfolio-participant-integration@movrit.local'
)
LEFT JOIN campaign_eligibility_requirement requirement ON requirement.campaign_id = campaign.id
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_application existing
    WHERE existing.campaign_id = campaign.id AND existing.creator_id = creator.id
);

-- 기존에 먼저 생성된 fixture에도 동일한 비민감 누출 검수 식별값을 채웁니다.
UPDATE campaign_application application
SET answers_json = '[{"questionId":"qa_private_portfolio_answer","value":"QA_PORTFOLIO_OTHER_BRAND_38_ONLY"}]',
    updated_at = NOW()
FROM collaboration_campaign campaign
WHERE campaign.id = application.campaign_id
  AND campaign.title = '[QA-PORTFOLIO] 타 브랜드 완료'
  AND application.answers_json <> '[{"questionId":"qa_private_portfolio_answer","value":"QA_PORTFOLIO_OTHER_BRAND_38_ONLY"}]';

INSERT INTO campaign_content_submission (
    application_id, content_url, note, status, revision_count,
    review_comment, submitted_at, reviewed_at, version, created_at, updated_at
)
SELECT application.id, fixture.content_url, '포트폴리오 공개 이력 검수',
       fixture.content_status, fixture.revision_count, '검수 완료',
       NOW() - INTERVAL '25 days', NOW() - INTERVAL '19 days', 0, NOW(), NOW()
FROM campaign_application application
JOIN collaboration_campaign campaign ON campaign.id = application.campaign_id
JOIN (VALUES
    ('[QA-PORTFOLIO] 완료 이력', 'https://instagram.com/p/movrit_portfolio_done', 'APPROVED', 2),
    ('[QA-PORTFOLIO] 미완료 이력', 'https://instagram.com/p/movrit_portfolio_pending', 'BRAND_APPROVED', 0),
    ('[QA-PORTFOLIO] 타 브랜드 완료', 'https://blog.naver.com/movrit/portfolio-done', 'APPROVED', 0)
) AS fixture(campaign_title, content_url, content_status, revision_count)
  ON fixture.campaign_title = campaign.title
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_content_submission existing
    WHERE existing.application_id = application.id
);

INSERT INTO campaign_content_link (
    submission_id, channel_type, content_url, created_at, updated_at
)
SELECT submission.id, fixture.channel_type, fixture.content_url, NOW(), NOW()
FROM campaign_content_submission submission
JOIN campaign_application application ON application.id = submission.application_id
JOIN collaboration_campaign campaign ON campaign.id = application.campaign_id
JOIN (VALUES
    ('[QA-PORTFOLIO] 완료 이력', 'INSTAGRAM', 'https://instagram.com/p/movrit_portfolio_done'),
    ('[QA-PORTFOLIO] 완료 이력', 'BLOG', 'https://blog.naver.com/movrit/portfolio-review'),
    ('[QA-PORTFOLIO] 미완료 이력', 'INSTAGRAM', 'https://instagram.com/p/movrit_portfolio_pending'),
    ('[QA-PORTFOLIO] 타 브랜드 완료', 'BLOG', 'https://blog.naver.com/movrit/portfolio-done')
) AS fixture(campaign_title, channel_type, content_url) ON fixture.campaign_title = campaign.title
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_content_link existing
    WHERE existing.submission_id = submission.id
      AND existing.channel_type = fixture.channel_type
      AND existing.content_url = fixture.content_url
);

-- 현재 로컬 브라우저 사용자 user_17이 콘텐츠 제출 화면을 직접 확인하는 독립 fixture입니다.
-- 기존 신청 24와 캠페인 11은 수정하지 않으며 콘텐츠 제출 행도 미리 만들지 않습니다.

INSERT INTO preorder (
    product_id, title, description, preorder_price, currency,
    target_quantity, max_quantity, ordered_quantity, start_at, end_at,
    status, version, created_at, updated_at, min_order_quantity,
    max_order_quantity_per_member
)
SELECT product.id, '[테스트] 콘텐츠 제출 체험 연결 프리오더',
       '사용자가 직접 콘텐츠 URL을 제출하는 로컬 검수 전용 연결 데이터',
       9000, 'KRW', 10, 100, 0,
       NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days',
       'OPEN', 0, NOW(), NOW(), 1, 1
FROM product
JOIN brand ON brand.id = product.brand_id
WHERE product.name = '[QA-WORKSPACE] 콘텐츠 제출 검수 상품'
  AND brand.name = 'QA WORKSPACE'
  AND NOT EXISTS (
      SELECT 1 FROM preorder
      WHERE preorder.title = '[테스트] 콘텐츠 제출 체험 연결 프리오더'
  );

INSERT INTO collaboration_campaign (
    preorder_id, title, description, category, min_follower_count,
    recruitment_count, reward_description, start_at, end_at, open,
    mission, channel_guide, schedule_guide, caution, submission_requirement,
    selection_announced_at, experience_start_at, experience_end_at,
    content_submission_start_at, content_submission_end_at, results_announced_at,
    schedule_version, representative_image_url, contact_name, contact_phone,
    created_at, updated_at
)
SELECT preorder.id, '[테스트] 콘텐츠 제출 체험',
       '현재 로그인 사용자가 채널별 콘텐츠 링크를 직접 제출하는 합성 캠페인입니다.',
       '생활', 0, 5, '로컬 검수용 가상 체험',
       NOW() - INTERVAL '20 days', NOW() - INTERVAL '10 days', FALSE,
       'Instagram 등 본인이 공개한 테스트 게시물 URL을 직접 등록해 주세요.',
       '외부 게시물 자동 수집 없이 입력한 URL 형식만 검사합니다.',
       '현재 콘텐츠 제출 기간이 열려 있습니다.',
       '실제 개인정보·결제·상품 구매 정보를 입력하지 마세요.',
       '공개 테스트 게시물 URL을 채널 유형과 함께 제출해 주세요.',
       NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days',
       NOW() - INTERVAL '1 day', NOW() + INTERVAL '14 days', NOW() + INTERVAL '16 days',
       1, '/assets/discovery/banners/living.png',
       'QA 콘텐츠 담당자', '02-0000-0017', NOW(), NOW()
FROM preorder
WHERE preorder.title = '[테스트] 콘텐츠 제출 체험 연결 프리오더'
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
WHERE campaign.title = '[테스트] 콘텐츠 제출 체험'
ON CONFLICT (campaign_id) DO NOTHING;

INSERT INTO campaign_application (
    campaign_id, creator_id, fulfillment_type, status, message,
    recipient_name, recipient_phone, postal_code, address1, address2,
    answers_json, agreement_acceptances_json, form_version,
    identity_verification_configured, identity_verified, email_verified,
    channel_ready, submitted_at, responded_at, experience_confirmed_at,
    created_at, updated_at
)
SELECT campaign.id, source.creator_id, 'DELIVERY', 'ACTIVE',
       '사용자가 직접 콘텐츠를 제출할 수 있도록 준비된 테스트 신청입니다.',
       'QA 사용자', '010-0000-0017', '00000', '서울특별시 테스트구 검수로 17', 'QA',
       '[]', '[]', 1, FALSE, FALSE, TRUE, TRUE,
       NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days',
       NOW(), NOW()
FROM collaboration_campaign campaign
CROSS JOIN (
    SELECT application.creator_id
    FROM campaign_application application
    WHERE application.id = 24
) source
WHERE campaign.title = '[테스트] 콘텐츠 제출 체험'
  AND NOT EXISTS (
      SELECT 1 FROM campaign_application existing
      WHERE existing.campaign_id = campaign.id
        AND existing.creator_id = source.creator_id
  );

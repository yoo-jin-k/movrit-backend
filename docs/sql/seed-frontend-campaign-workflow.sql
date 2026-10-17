-- 판매자 4의 캠페인 화면을 바로 확인할 수 있도록 신청 폼과 상태별 예시를 채웁니다.
UPDATE collaboration_campaign
SET mission = CASE id
        WHEN 9 THEN '세럼 사용 전후 피부 표현을 자연광 사진 5장 이상으로 소개해 주세요.'
        WHEN 10 THEN '아침 또는 저녁 루틴에 2주간 사용한 솔직한 변화를 기록해 주세요.'
        WHEN 11 THEN '크루 러닝 현장과 착용 모습을 사진 및 짧은 영상으로 남겨 주세요.'
        WHEN 12 THEN '라이브 또는 게시물에서 제품 특징과 프리오더 일정을 안내해 주세요.'
    END,
    channel_guide = CASE id WHEN 11 THEN '인스타그램 릴스 또는 유튜브 쇼츠' ELSE '인스타그램 피드 또는 블로그' END,
    schedule_guide = '선정 알림 후 3일 안에 참여를 확정하고 체험 종료 후 7일 안에 제출해 주세요.',
    delivery_guide = CASE WHEN id = 11 THEN NULL ELSE '선정된 주소로 샘플을 무료 배송합니다.' END,
    visit_guide = CASE WHEN id = 11 THEN '서울 러닝 베이스에서 토요일 오전 9시에 진행합니다.' ELSE NULL END,
    caution = '타인의 사진을 사용하거나 확인되지 않은 효능을 단정해서 표현하지 마세요.',
    submission_requirement = '게시물 공개 상태를 유지하고 최종 URL을 제출해 주세요.'
WHERE id IN (9, 10, 11, 12);

INSERT INTO campaign_application_form (
    campaign_id, allowed_fulfillment_types_json, questions_json, agreements_json,
    version, created_at, updated_at
)
SELECT campaign.id,
       CASE WHEN campaign.id = 11 THEN '["VISIT"]' ELSE '["DELIVERY"]' END,
       '[{"id":"experience","label":"관련 경험을 알려 주세요.","required":true,"type":"TEXTAREA","options":[],"maxLength":1000},{"id":"plan","label":"콘텐츠 계획을 알려 주세요.","required":true,"type":"TEXTAREA","options":[],"maxLength":1000}]',
       '[{"id":"privacy","title":"개인정보 수집 및 이용","version":"2026-10-01","required":true,"body":"캠페인 운영 및 상품 배송을 위해 신청 정보와 배송 정보를 수집합니다."}]',
       1, NOW(), NOW()
FROM collaboration_campaign campaign
WHERE campaign.id IN (9, 10, 11, 12)
  AND NOT EXISTS (
      SELECT 1 FROM campaign_application_form form WHERE form.campaign_id = campaign.id
  );

INSERT INTO campaign_application (
    campaign_id, creator_id, fulfillment_type, status, message,
    recipient_name, recipient_phone, postal_code, address1, address2,
    answers_json, agreement_acceptances_json, form_version,
    identity_verification_configured, identity_verified, channel_ready,
    submitted_at, responded_at, cancelled_at, cancellation_reason,
    experience_confirmed_at, created_at, updated_at
)
SELECT values_to_add.campaign_id,
       values_to_add.creator_id,
       values_to_add.fulfillment_type,
       values_to_add.status,
       values_to_add.message,
       CASE WHEN values_to_add.fulfillment_type = 'DELIVERY' THEN '김체험' END,
       CASE WHEN values_to_add.fulfillment_type = 'DELIVERY' THEN '010-1234-5678' END,
       CASE WHEN values_to_add.fulfillment_type = 'DELIVERY' THEN '06236' END,
       CASE WHEN values_to_add.fulfillment_type = 'DELIVERY' THEN '서울특별시 강남구 테헤란로 1' END,
       CASE WHEN values_to_add.fulfillment_type = 'DELIVERY' THEN '101호' END,
       '[{"questionId":"experience","value":"관련 카테고리 리뷰를 꾸준히 작성했습니다."},{"questionId":"plan","value":"사용 과정과 장점을 사진 중심으로 소개하겠습니다."}]',
       '[{"agreementId":"privacy","version":"2026-10-01","accepted":true}]',
       1, false, false, true,
       NOW() - INTERVAL '5 days',
       CASE WHEN values_to_add.status = 'SUBMITTED' THEN NULL ELSE NOW() - INTERVAL '4 days' END,
       CASE WHEN values_to_add.status = 'CANCELLED' THEN NOW() - INTERVAL '1 day' END,
       CASE WHEN values_to_add.status = 'CANCELLED' THEN '개인 일정으로 참여가 어렵습니다.' END,
       CASE WHEN values_to_add.status IN ('CONTENT_SUBMITTED', 'REVISION_REQUESTED', 'COMPLETED')
            THEN NOW() - INTERVAL '2 days' END,
       NOW() - INTERVAL '5 days', NOW()
FROM (VALUES
    (9, 5, 'DELIVERY', 'SUBMITTED', '뷰티 콘텐츠 경험을 바탕으로 꼼꼼하게 리뷰하겠습니다.'),
    (9, 6, 'DELIVERY', 'SELECTED', '운동 후 피부 진정 루틴과 함께 소개하겠습니다.'),
    (10, 5, 'DELIVERY', 'CONTENT_SUBMITTED', '2주 동안 매일 사용하고 변화를 기록하겠습니다.'),
    (10, 6, 'DELIVERY', 'REVISION_REQUESTED', '건강한 아침 루틴 콘텐츠로 제작하겠습니다.'),
    (11, 5, 'VISIT', 'ACTIVE', '러닝 현장의 분위기를 생생하게 담겠습니다.'),
    (11, 6, 'VISIT', 'COMPLETED', '러닝 크루와 함께 참여하고 쇼츠로 제작하겠습니다.'),
    (12, 5, 'DELIVERY', 'CANCELLED', '공동구매 일정에 맞춰 콘텐츠를 준비하겠습니다.')
) AS values_to_add(campaign_id, creator_id, fulfillment_type, status, message)
WHERE NOT EXISTS (
    SELECT 1
    FROM campaign_application application
    WHERE application.campaign_id = values_to_add.campaign_id
      AND application.creator_id = values_to_add.creator_id
);

INSERT INTO campaign_content_submission (
    application_id, content_url, note, status, revision_count,
    review_comment, submitted_at, reviewed_at, created_at, updated_at
)
SELECT application.id,
       'https://example.com/movrit/campaign-content-' || application.id,
       '프론트 연동 확인용 콘텐츠입니다.',
       CASE application.status
           WHEN 'CONTENT_SUBMITTED' THEN 'SUBMITTED'
           WHEN 'REVISION_REQUESTED' THEN 'REVISION_REQUESTED'
           ELSE 'APPROVED'
       END,
       CASE WHEN application.status = 'REVISION_REQUESTED' THEN 1 ELSE 0 END,
       CASE
           WHEN application.status = 'REVISION_REQUESTED' THEN '필수 안내 문구를 본문에 추가해 주세요.'
           WHEN application.status = 'COMPLETED' THEN '검토가 완료되었습니다.'
       END,
       NOW() - INTERVAL '1 day',
       CASE WHEN application.status = 'CONTENT_SUBMITTED' THEN NULL ELSE NOW() END,
       NOW() - INTERVAL '1 day', NOW()
FROM campaign_application application
WHERE application.status IN ('CONTENT_SUBMITTED', 'REVISION_REQUESTED', 'COMPLETED')
  AND application.campaign_id IN (9, 10, 11, 12)
  AND NOT EXISTS (
      SELECT 1 FROM campaign_content_submission submission
      WHERE submission.application_id = application.id
  );

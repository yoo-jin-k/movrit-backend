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

-- 같은 판매자의 상품 상세와 재고 화면에서 옵션별 상태를 확인할 수 있게 보강합니다.
INSERT INTO product_variant (
    product_id, sku_code, option_name_1, option_value_1,
    additional_price, on_hand_quantity, reserved_quantity,
    safety_stock_quantity, sold_quantity, active, version, created_at, updated_at
)
SELECT product.id, variant.sku_code, variant.option_name, variant.option_value,
       variant.additional_price, variant.on_hand_quantity, variant.reserved_quantity,
       variant.safety_stock_quantity, variant.sold_quantity, variant.active, 0, NOW(), NOW()
FROM (VALUES
    ('에어핏 러닝 재킷', 'LUMENA-JACKET-S', '사이즈', 'S', 0::numeric, 42::bigint, 3::bigint, 5::bigint, 18::bigint, true),
    ('에어핏 러닝 재킷', 'LUMENA-JACKET-L', '사이즈', 'L', 5000::numeric, 7::bigint, 5::bigint, 5::bigint, 36::bigint, true),
    ('시카 데일리 선크림', 'LUMENA-SUN-DUO', '구성', '2개입', 18000::numeric, 65::bigint, 8::bigint, 10::bigint, 52::bigint, true)
) AS variant(product_name, sku_code, option_name, option_value, additional_price,
             on_hand_quantity, reserved_quantity, safety_stock_quantity, sold_quantity, active)
JOIN product ON product.name = variant.product_name AND product.brand_id = 4
WHERE NOT EXISTS (
    SELECT 1 FROM product_variant existing WHERE existing.sku_code = variant.sku_code
);

INSERT INTO preorder_variant (
    id, preorder_id, product_variant_id, preorder_price, target_quantity,
    max_quantity, ordered_quantity, status, created_at, updated_at
)
SELECT nextval('preorder_variant_id_seq'), preorder.id, product_variant.id,
       preorder.preorder_price + product_variant.additional_price,
       GREATEST(10, preorder.target_quantity / 3),
       LEAST(product_variant.on_hand_quantity, COALESCE(preorder.max_quantity, product_variant.on_hand_quantity)),
       product_variant.sold_quantity, 'ACTIVE', NOW(), NOW()
FROM preorder
JOIN product_variant ON product_variant.product_id = preorder.product_id
JOIN product ON product.id = preorder.product_id
WHERE product.brand_id = 4
  AND NOT EXISTS (
      SELECT 1 FROM preorder_variant existing
      WHERE existing.preorder_id = preorder.id
        AND existing.product_variant_id = product_variant.id
  );

-- 찜과 알림 신청은 목록 카드의 카운트·선택 상태를 확인하는 데 사용합니다.
INSERT INTO preorder_favorite (member_id, preorder_id, created_at, updated_at)
SELECT sample.member_id, sample.preorder_id, NOW() - INTERVAL '2 days', NOW()
FROM (VALUES (1::bigint, 9::bigint), (2::bigint, 9::bigint), (1::bigint, 10::bigint),
             (3::bigint, 11::bigint), (2::bigint, 12::bigint)) AS sample(member_id, preorder_id)
WHERE EXISTS (SELECT 1 FROM member WHERE id = sample.member_id)
  AND EXISTS (SELECT 1 FROM preorder WHERE id = sample.preorder_id)
ON CONFLICT (member_id, preorder_id) DO NOTHING;

INSERT INTO preorder_interest (preorder_id, member_id, created_at)
SELECT sample.preorder_id, sample.member_id, NOW() - INTERVAL '1 day'
FROM (VALUES (9::bigint, 1::bigint), (10::bigint, 2::bigint),
             (11::bigint, 3::bigint), (12::bigint, 1::bigint)) AS sample(preorder_id, member_id)
WHERE EXISTS (SELECT 1 FROM member WHERE id = sample.member_id)
  AND EXISTS (SELECT 1 FROM preorder WHERE id = sample.preorder_id)
ON CONFLICT (preorder_id, member_id) DO NOTHING;

-- 답변 전/완료 문의를 함께 두어 판매자 문의함의 상태 필터를 바로 점검할 수 있게 합니다.
INSERT INTO inquiry (
    id, member_id, preorder_id, category, title, content, status,
    answer, answered_by_member_id, answered_at, created_at, updated_at
)
SELECT nextval('inquiry_id_seq'), sample.member_id, sample.preorder_id, sample.category,
       sample.title, sample.content, sample.status, sample.answer,
       CASE WHEN sample.answer IS NULL THEN NULL ELSE (
           SELECT seller_profile.member_id FROM seller_profile WHERE seller_profile.id = 4
       ) END,
       CASE WHEN sample.answer IS NULL THEN NULL ELSE NOW() - INTERVAL '6 hours' END,
       NOW() - sample.age, NOW()
FROM (VALUES
    (1::bigint, 9::bigint, 'PRODUCT', '민감성 피부도 사용할 수 있나요?',
     '향료 포함 여부와 사용 시 주의사항이 궁금합니다.', 'OPEN', NULL::text, INTERVAL '12 hours'),
    (2::bigint, 10::bigint, 'DELIVERY', '배송 예정일을 알고 싶어요',
     '펀딩 성공 후 언제부터 순차 배송되나요?', 'ANSWERED',
     '생산 완료 후 안내된 일정에 맞춰 순차 발송할 예정입니다.', INTERVAL '2 days')
) AS sample(member_id, preorder_id, category, title, content, status, answer, age)
WHERE EXISTS (SELECT 1 FROM member WHERE id = sample.member_id)
  AND EXISTS (SELECT 1 FROM preorder WHERE id = sample.preorder_id)
  AND NOT EXISTS (
      SELECT 1 FROM inquiry existing
      WHERE existing.member_id = sample.member_id
        AND existing.preorder_id = sample.preorder_id
        AND existing.title = sample.title
  );

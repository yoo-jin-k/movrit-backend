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

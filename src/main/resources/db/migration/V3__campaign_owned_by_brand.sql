-- 캠페인이 프리오더 없이도 브랜드에 직접 속하도록 바꿉니다. 2026-10-31
-- 지금까지는 캠페인 → 프리오더 → 상품 → 브랜드 경로로만 운영 브랜드를 알 수 있어, 프리오더가 없는 브랜드는 캠페인을 만들 수 없었습니다.
-- 기존 캠페인은 연결된 프리오더의 브랜드로 brand_id를 채우고 프리오더 연결은 그대로 보존합니다.

-- 1) 운영 브랜드 컬럼을 추가합니다.
ALTER TABLE collaboration_campaign ADD COLUMN brand_id bigint;

-- 2) 기존 캠페인은 연결된 프리오더 상품의 브랜드로 채웁니다.
UPDATE collaboration_campaign c
SET brand_id = p.brand_id
FROM preorder pr
JOIN product p ON p.id = pr.product_id
WHERE pr.id = c.preorder_id
  AND c.brand_id IS NULL;

-- 3) 모든 캠페인은 운영 브랜드가 있어야 합니다. 채우지 못한 행이 있으면 여기서 실패해 적용 전체가 취소됩니다.
ALTER TABLE collaboration_campaign ALTER COLUMN brand_id SET NOT NULL;
ALTER TABLE collaboration_campaign
  ADD CONSTRAINT fk_collaboration_campaign_brand FOREIGN KEY (brand_id) REFERENCES brand(id);
CREATE INDEX idx_collaboration_campaign_brand ON collaboration_campaign (brand_id);

-- 4) 프리오더 연결은 선택 항목이 됩니다. 연결된 경우 프리오더 하나에 캠페인 하나라는 기존 유일 제약은 유지합니다.
ALTER TABLE collaboration_campaign ALTER COLUMN preorder_id DROP NOT NULL;

-- 프리오더별로 실제 판매 가능한 SKU를 명시적으로 연결한다.
-- 같은 상품의 모든 SKU를 자동 허용하지 않아서 옵션별 판매 제외가 가능하다.

CREATE TABLE IF NOT EXISTS preorder_variant (
    preorder_id BIGINT NOT NULL REFERENCES preorder(id) ON DELETE CASCADE,
    product_variant_id BIGINT NOT NULL REFERENCES product_variant(id),
    PRIMARY KEY (preorder_id, product_variant_id)
);

CREATE INDEX IF NOT EXISTS idx_preorder_variant_variant_id
    ON preorder_variant(product_variant_id);

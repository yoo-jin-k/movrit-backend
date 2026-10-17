-- 2026-10-18 행위별 사용자 승인 후 로컬 movrit_integration에 적용했습니다.
-- 연결 7쌍 일치 및 기존 7행 전체/가격·order_item 전체·레거시 FK 불변을 확인했습니다.
-- QA-MEDIA-014: 판매 허용 SKU 연결과 레거시 판매 조건을 분리합니다.
-- 기존 preorder_variant의 가격, 수량, 상태 및 order_item 참조는 변경하지 않습니다.
-- 신규 매핑을 기동하기 전에 적용하며, 재실행 시 현재 연결 선택을 덮어쓰지 않습니다.
DO $$
BEGIN
    IF to_regclass('preorder_sale_variant') IS NULL THEN
        CREATE TABLE preorder_sale_variant (
            preorder_id BIGINT NOT NULL REFERENCES preorder(id) ON DELETE CASCADE,
            product_variant_id BIGINT NOT NULL REFERENCES product_variant(id),
            PRIMARY KEY (preorder_id, product_variant_id)
        );
        -- 최초 생성 때만 현재 코드가 읽던 연결을 복사합니다.
        -- 재실행으로 이후 제거한 SKU가 복원되지 않도록 합니다.
        IF to_regclass('preorder_variant') IS NOT NULL THEN
            INSERT INTO preorder_sale_variant (preorder_id, product_variant_id)
            SELECT preorder_id, product_variant_id FROM preorder_variant;
        END IF;
    END IF;
END
$$;
CREATE INDEX IF NOT EXISTS idx_preorder_sale_variant_variant_id
    ON preorder_sale_variant(product_variant_id);

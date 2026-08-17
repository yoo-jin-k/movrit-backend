-- MOVRIT feature/brand schema update
-- 현재 product / preorder 테스트 데이터가 없는 개발 DB 기준으로 작성됐다.
-- 운영 데이터가 생긴 뒤에는 DROP/NOT NULL 변경 전에 별도 데이터 이관 절차가 필요하다.

CREATE TABLE IF NOT EXISTS brand (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES seller_profile(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    cover_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_brand_seller_id ON brand(seller_id);

ALTER TABLE product
    DROP COLUMN IF EXISTS seller_id;

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS brand_id BIGINT,
    ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500);

ALTER TABLE product
    ADD CONSTRAINT fk_product_brand
    FOREIGN KEY (brand_id) REFERENCES brand(id);

-- 개발 DB에서 기존 상품 데이터가 없을 때 실행 가능하다.
ALTER TABLE product
    ALTER COLUMN brand_id SET NOT NULL;

CREATE TABLE IF NOT EXISTS product_variant (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id),
    sku_code VARCHAR(80) NOT NULL UNIQUE,
    option_name_1 VARCHAR(50),
    option_value_1 VARCHAR(100),
    option_name_2 VARCHAR(50),
    option_value_2 VARCHAR(100),
    additional_price NUMERIC(19, 2) NOT NULL,
    on_hand_quantity BIGINT NOT NULL,
    reserved_quantity BIGINT NOT NULL,
    safety_stock_quantity BIGINT NOT NULL,
    sold_quantity BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    version BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_variant_stock_non_negative CHECK (
        on_hand_quantity >= 0
        AND reserved_quantity >= 0
        AND safety_stock_quantity >= 0
        AND sold_quantity >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_product_variant_product_id ON product_variant(product_id);

ALTER TABLE preorder
    DROP COLUMN IF EXISTS seller_id;

ALTER TABLE preorder
    ADD COLUMN IF NOT EXISTS min_order_quantity BIGINT,
    ADD COLUMN IF NOT EXISTS max_order_quantity_per_member BIGINT;

UPDATE preorder
SET min_order_quantity = 1
WHERE min_order_quantity IS NULL;

ALTER TABLE preorder
    ALTER COLUMN min_order_quantity SET NOT NULL;

ALTER TABLE preorder
    ADD CONSTRAINT ck_preorder_min_order_positive CHECK (min_order_quantity >= 1);

ALTER TABLE preorder
    ADD CONSTRAINT ck_preorder_target_positive CHECK (target_quantity >= 1);

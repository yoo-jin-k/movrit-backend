ALTER TABLE member
    ADD COLUMN IF NOT EXISTS role VARCHAR(20);

UPDATE member
SET role = 'BUYER'
WHERE role IS NULL OR role NOT IN ('BUYER', 'CREATOR', 'BRAND', 'ADMIN');

ALTER TABLE member
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE influencer_profile
    ADD COLUMN IF NOT EXISTS channel_platform VARCHAR(30),
    ADD COLUMN IF NOT EXISTS channel_handle VARCHAR(100),
    ADD COLUMN IF NOT EXISTS channel_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS follower_count BIGINT,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);

UPDATE influencer_profile
SET status = 'PENDING'
WHERE status IS NULL
   OR status NOT IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED');

ALTER TABLE influencer_profile
    ALTER COLUMN status SET NOT NULL;

CREATE TABLE IF NOT EXISTS product_sku (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(100) NOT NULL,
    option_name VARCHAR(200) NOT NULL,
    additional_price NUMERIC(19,2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_sku_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT uk_product_sku_code UNIQUE (sku_code)
);

CREATE TABLE IF NOT EXISTS inventory (
    id BIGSERIAL PRIMARY KEY,
    product_sku_id BIGINT NOT NULL,
    on_hand_quantity BIGINT NOT NULL,
    reserved_quantity BIGINT NOT NULL,
    safety_stock_quantity BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product_sku FOREIGN KEY (product_sku_id) REFERENCES product_sku(id),
    CONSTRAINT uk_inventory_product_sku UNIQUE (product_sku_id),
    CONSTRAINT ck_inventory_quantities CHECK (on_hand_quantity >= 0 AND reserved_quantity >= 0 AND safety_stock_quantity >= 0)
);

CREATE TABLE IF NOT EXISTS preorder_variant (
    id BIGSERIAL PRIMARY KEY,
    preorder_id BIGINT NOT NULL,
    product_sku_id BIGINT NOT NULL,
    sale_price NUMERIC(19,2) NOT NULL,
    max_quantity BIGINT,
    ordered_quantity BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_preorder_variant_preorder FOREIGN KEY (preorder_id) REFERENCES preorder(id),
    CONSTRAINT fk_preorder_variant_product_sku FOREIGN KEY (product_sku_id) REFERENCES product_sku(id),
    CONSTRAINT uk_preorder_variant UNIQUE (preorder_id, product_sku_id),
    CONSTRAINT ck_preorder_variant_quantity CHECK (ordered_quantity >= 0 AND (max_quantity IS NULL OR max_quantity > 0))
);

CREATE TABLE IF NOT EXISTS influencer_campaign (
    id BIGSERIAL PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    preorder_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    commission_rate NUMERIC(5, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_influencer_campaign_creator
        FOREIGN KEY (creator_id) REFERENCES influencer_profile(id),
    CONSTRAINT fk_influencer_campaign_preorder
        FOREIGN KEY (preorder_id) REFERENCES preorder(id),
    CONSTRAINT uk_influencer_campaign_creator_preorder
        UNIQUE (creator_id, preorder_id),
    CONSTRAINT ck_influencer_campaign_commission_rate
        CHECK (commission_rate >= 0 AND commission_rate <= 100)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_order_number_idx ON orders(order_number);
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_request_key_idx ON orders(request_key);

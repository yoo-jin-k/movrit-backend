-- 주문부터 정산까지의 상태 전이에서 중복 생성을 막는 제약이다.
-- 기존 중복 데이터가 있다면 먼저 정리한 뒤 실행해야 한다.

CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_order_number ON orders(order_number);
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_request_key ON orders(request_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_order_id ON payment(order_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_idempotency_key
    ON payment(idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_payment_key
    ON payment(payment_key)
    WHERE payment_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_shipment_order_id ON shipment(order_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_settlement_preorder_id ON settlement(preorder_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_settlement_number ON settlement(settlement_number);

ALTER TABLE orders
    ADD CONSTRAINT ck_orders_amount_non_negative CHECK (
        total_product_amount >= 0
        AND discount_amount >= 0
        AND shipping_amount >= 0
        AND total_payment_amount >= 0
    );

ALTER TABLE payment
    ADD CONSTRAINT ck_payment_amount_non_negative CHECK (
        requested_amount >= 0
        AND (approved_amount IS NULL OR approved_amount >= 0)
    );

ALTER TABLE settlement
    ADD CONSTRAINT ck_settlement_amount_non_negative CHECK (
        gross_amount >= 0
        AND refund_amount >= 0
        AND platform_fee_amount >= 0
        AND net_amount >= 0
    );

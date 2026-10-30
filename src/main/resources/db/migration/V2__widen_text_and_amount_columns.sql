-- 두 DB(movrit, movrit_integration)가 서로 다르게 좁혀 둔 컬럼을 넓은 쪽 기준으로 통일합니다. 2026-10-31
-- 이미 같은 길이인 DB에서는 변화가 없고, 데이터는 줄이거나 지우지 않습니다.

-- 신청 시점 배송지 사본은 주소록(member_address, 300자)과 같은 길이여야 긴 주소로 신청할 때 저장 오류가 나지 않습니다.
ALTER TABLE campaign_application ALTER COLUMN address1 TYPE varchar(300);
ALTER TABLE campaign_application ALTER COLUMN address2 TYPE varchar(300);

-- 취소·실패 사유와 감사 기록 사유는 문장 단위 입력이라 500자까지 저장합니다.
ALTER TABLE campaign_application ALTER COLUMN cancellation_reason TYPE varchar(500);
ALTER TABLE preorder ALTER COLUMN cancellation_reason TYPE varchar(500);
ALTER TABLE payment ALTER COLUMN failure_message TYPE varchar(500);
ALTER TABLE audit_log ALTER COLUMN reason TYPE varchar(500);

-- 알림 본문은 캠페인 이름·수정 의견이 포함될 수 있어 1000자까지 저장합니다.
ALTER TABLE notification ALTER COLUMN message TYPE varchar(1000);

-- 금액 컬럼은 정밀도를 numeric(38,2)로 통일합니다(소수 둘째 자리 유지, 범위만 확장).
ALTER TABLE order_item ALTER COLUMN unit_price TYPE numeric(38,2);
ALTER TABLE order_item ALTER COLUMN total_price TYPE numeric(38,2);
ALTER TABLE orders ALTER COLUMN total_product_amount TYPE numeric(38,2);
ALTER TABLE orders ALTER COLUMN discount_amount TYPE numeric(38,2);
ALTER TABLE orders ALTER COLUMN shipping_amount TYPE numeric(38,2);
ALTER TABLE orders ALTER COLUMN total_payment_amount TYPE numeric(38,2);
ALTER TABLE payment ALTER COLUMN requested_amount TYPE numeric(38,2);
ALTER TABLE payment ALTER COLUMN approved_amount TYPE numeric(38,2);
ALTER TABLE preorder ALTER COLUMN preorder_price TYPE numeric(38,2);
ALTER TABLE product ALTER COLUMN normal_price TYPE numeric(38,2);
ALTER TABLE product_variant ALTER COLUMN additional_price TYPE numeric(38,2);
ALTER TABLE settlement ALTER COLUMN gross_amount TYPE numeric(38,2);
ALTER TABLE settlement ALTER COLUMN refund_amount TYPE numeric(38,2);
ALTER TABLE settlement ALTER COLUMN platform_fee_amount TYPE numeric(38,2);
ALTER TABLE settlement ALTER COLUMN net_amount TYPE numeric(38,2);

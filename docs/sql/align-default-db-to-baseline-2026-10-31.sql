-- 기본 개발 DB(movrit)를 Flyway 기준 스키마(V1, 통합 DB 기준)에 맞추는 1회성 정렬 스크립트입니다. 2026-10-31
-- 통합 DB보다 짧은 문자열 컬럼만 넓히고, 없는 제약 2개를 추가합니다. 데이터 삭제·축소는 하지 않습니다.
-- 적용 대상: movrit(기본 프로필) 한 번만. 통합 DB에는 적용하지 않습니다.

ALTER TABLE audit_log ALTER COLUMN action TYPE varchar(255);
ALTER TABLE audit_log ALTER COLUMN target_type TYPE varchar(255);
ALTER TABLE campaign_application ALTER COLUMN fulfillment_type TYPE varchar(255);
ALTER TABLE campaign_application ALTER COLUMN status TYPE varchar(255);
ALTER TABLE campaign_application ALTER COLUMN recipient_name TYPE varchar(255);
ALTER TABLE campaign_application ALTER COLUMN recipient_phone TYPE varchar(255);
ALTER TABLE campaign_application ALTER COLUMN postal_code TYPE varchar(255);
ALTER TABLE campaign_content_submission ALTER COLUMN status TYPE varchar(255);
ALTER TABLE influencer_profile ALTER COLUMN nickname TYPE varchar(255);
ALTER TABLE influencer_profile ALTER COLUMN primary_category TYPE varchar(255);
ALTER TABLE influencer_profile ALTER COLUMN status TYPE varchar(255);
ALTER TABLE inquiry ALTER COLUMN category TYPE varchar(255);
ALTER TABLE inquiry ALTER COLUMN title TYPE varchar(255);
ALTER TABLE inquiry ALTER COLUMN status TYPE varchar(255);
ALTER TABLE member_address ALTER COLUMN address1 TYPE varchar(300);
ALTER TABLE member_address ALTER COLUMN address2 TYPE varchar(300);
ALTER TABLE notification ALTER COLUMN type TYPE varchar(255);
ALTER TABLE notification ALTER COLUMN title TYPE varchar(255);
ALTER TABLE notification ALTER COLUMN target_type TYPE varchar(255);
ALTER TABLE order_item ALTER COLUMN product_name TYPE varchar(255);
ALTER TABLE order_item ALTER COLUMN option_name TYPE varchar(255);
ALTER TABLE orders ALTER COLUMN order_number TYPE varchar(255);
ALTER TABLE orders ALTER COLUMN request_key TYPE varchar(255);
ALTER TABLE orders ALTER COLUMN acquisition_source TYPE varchar(255);
ALTER TABLE orders ALTER COLUMN status TYPE varchar(255);
ALTER TABLE orders ALTER COLUMN receiver_name TYPE varchar(255);
ALTER TABLE orders ALTER COLUMN receiver_phone TYPE varchar(255);
ALTER TABLE orders ALTER COLUMN postal_code TYPE varchar(255);
ALTER TABLE payment ALTER COLUMN pg_provider TYPE varchar(255);
ALTER TABLE payment ALTER COLUMN method TYPE varchar(255);
ALTER TABLE payment ALTER COLUMN status TYPE varchar(255);
ALTER TABLE payment ALTER COLUMN idempotency_key TYPE varchar(255);
ALTER TABLE payment ALTER COLUMN failure_code TYPE varchar(255);
ALTER TABLE preorder ALTER COLUMN title TYPE varchar(255);
ALTER TABLE preorder ALTER COLUMN status TYPE varchar(255);
ALTER TABLE product ALTER COLUMN name TYPE varchar(255);
ALTER TABLE product ALTER COLUMN category TYPE varchar(255);
ALTER TABLE product ALTER COLUMN status TYPE varchar(255);
ALTER TABLE seller_profile ALTER COLUMN business_name TYPE varchar(255);
ALTER TABLE seller_profile ALTER COLUMN brand_name TYPE varchar(255);
ALTER TABLE seller_profile ALTER COLUMN business_number TYPE varchar(255);
ALTER TABLE seller_profile ALTER COLUMN representative_name TYPE varchar(255);
ALTER TABLE seller_profile ALTER COLUMN settlement_bank_code TYPE varchar(255);
ALTER TABLE seller_profile ALTER COLUMN settlement_account_masked TYPE varchar(255);
ALTER TABLE settlement ALTER COLUMN settlement_number TYPE varchar(255);
ALTER TABLE settlement ALTER COLUMN status TYPE varchar(255);
ALTER TABLE shipment ALTER COLUMN courier_code TYPE varchar(255);
ALTER TABLE shipment ALTER COLUMN tracking_number TYPE varchar(255);
ALTER TABLE shipment ALTER COLUMN status TYPE varchar(255);

-- 같은 인플루언서가 같은 프리오더 협업에 중복 지원하지 않도록 통합 DB와 같은 유일 제약을 둡니다.
ALTER TABLE collaboration ADD CONSTRAINT uk_collaboration_preorder_creator UNIQUE (preorder_id, creator_id);
-- 프리오더 판매 옵션이 실제 상품 옵션을 가리키도록 외래 키를 둡니다.
ALTER TABLE preorder_variant ADD CONSTRAINT fk_preorder_variant_product_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant(id);

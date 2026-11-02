-- 구매평 캠페인을 추가합니다. 2026-11-02
--
-- 구매평 캠페인은 참여자가 외부 쇼핑몰(네이버 스마트스토어·쿠팡 등)에서 상품을 직접 결제해 구매하고,
-- 구매처에 구매평(리뷰)을 남기면 브랜드가 약속한 금액을 환급(페이백)해 주는 방식입니다.
-- MOVRIT은 결제·송금을 처리하지 않고, 구매 인증과 환급 완료 여부만 기록합니다.

-- 1) 캠페인 유형에 PURCHASE_REVIEW(구매평)를 추가합니다.
ALTER TABLE collaboration_campaign DROP CONSTRAINT ck_collaboration_campaign_type;
ALTER TABLE collaboration_campaign
  ADD CONSTRAINT ck_collaboration_campaign_type
  CHECK (campaign_type IN ('PRODUCT', 'LOCAL', 'REPORTER', 'PURCHASE_REVIEW'));

-- 2) 구매평 캠페인의 구매 안내(캠페인당 1건)
CREATE TABLE campaign_purchase_review (
  campaign_id bigint PRIMARY KEY REFERENCES collaboration_campaign (id),
  -- 구매처 이름(예: 네이버 스마트스토어, 쿠팡)
  store_name varchar(50) NOT NULL,
  -- 참여자가 구매할 상품 페이지 주소(http·https)
  product_url varchar(500) NOT NULL,
  -- 구매처 판매가(원)
  product_price integer NOT NULL CHECK (product_price >= 0),
  -- 구매평 승인 후 브랜드가 돌려주는 금액(원). 판매가보다 클 수 없습니다.
  payback_amount integer NOT NULL CHECK (payback_amount >= 0),
  -- 구매 방법·옵션·구매평 작성 안내
  purchase_guide varchar(2000),
  updated_at timestamp(6) NOT NULL,
  CONSTRAINT ck_campaign_purchase_review_payback CHECK (payback_amount <= product_price)
);

-- 3) 참여자의 구매 인증과 환급 기록(신청당 1건)
CREATE TABLE campaign_purchase_proof (
  application_id bigint PRIMARY KEY REFERENCES campaign_application (id),
  -- 구매처 주문번호
  order_number varchar(60) NOT NULL,
  -- 실제 결제한 금액(원)
  paid_amount integer NOT NULL CHECK (paid_amount >= 0),
  -- 주문 내역 캡처 저장 키(uploads/purchase-proof). 공개 주소 없이 권한 있는 API로만 내려갑니다.
  evidence_key varchar(80) NOT NULL,
  submitted_at timestamp(6) NOT NULL,
  -- 환급 상태: PENDING(환급 대기) → PAID(환급 완료)
  payback_status varchar(20) NOT NULL DEFAULT 'PENDING',
  payback_paid_at timestamp(6),
  -- 환급 처리 메모(예: 계좌 이체 완료, 포인트 지급)
  payback_note varchar(200),
  CONSTRAINT ck_campaign_purchase_proof_payback CHECK (payback_status IN ('PENDING', 'PAID'))
);

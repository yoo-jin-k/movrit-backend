-- seed-integration-seller-revocation.sql로 기준 상태를 만든 뒤에만 실행합니다.
UPDATE seller_profile
SET status = 'REJECTED', updated_at = NOW()
WHERE business_number = 'QA-REVOCABLE-SELLER';

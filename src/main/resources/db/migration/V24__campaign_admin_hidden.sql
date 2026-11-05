-- 운영자 숨김 표시: 운영자가 숨긴 캠페인은 업체 마감(open = false)과 달리 상세 주소로 들어와도 일반 회원에게 보이지 않아야 합니다.
-- - 숨긴 시각을 남겨 업체 마감과 구분합니다. 다시 공개하면 비웁니다.
-- - 숨긴 동안에도 소유 업체·운영자·이미 신청한 참여자는 상세를 볼 수 있습니다(진행 중인 참여는 그대로 이어집니다).
ALTER TABLE collaboration_campaign ADD COLUMN admin_hidden_at timestamp(6);

COMMENT ON COLUMN collaboration_campaign.admin_hidden_at IS '운영자가 숨긴 시각입니다. 값이 있으면 소유 업체·운영자·신청자 외에는 상세를 볼 수 없습니다.';

-- 기간 연장이 승인되면 그 신청의 체험 기간도 함께 늘어납니다(콘텐츠 제출 마감은 V18의 content_deadline_extended_until).
-- 비어 있으면 캠페인의 체험 종료 시각을 그대로 씁니다. 다른 참여자와 캠페인 일정은 바뀌지 않습니다.
ALTER TABLE campaign_application ADD COLUMN experience_extended_until timestamp(6);

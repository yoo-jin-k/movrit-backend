-- 통합 QA 전용 DB에서만 사용하는 일정 예시입니다. 운영 일정으로 사용하지 않습니다.
UPDATE collaboration_campaign
SET selection_announced_at = end_at + INTERVAL '1 day',
    experience_start_at = end_at + INTERVAL '2 days',
    experience_end_at = end_at + INTERVAL '9 days',
    content_submission_start_at = end_at + INTERVAL '3 days',
    content_submission_end_at = end_at + INTERVAL '12 days',
    results_announced_at = end_at + INTERVAL '13 days',
    schedule_version = schedule_version + 1,
    updated_at = NOW()
WHERE id IN (9, 10, 11, 12)
  AND selection_announced_at IS NULL;

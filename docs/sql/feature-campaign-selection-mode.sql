-- 기존 캠페인은 브랜드 직접 선정으로 유지하고 신규 캠페인부터 선정 주체를 선택합니다.
ALTER TABLE collaboration_campaign
    ADD COLUMN IF NOT EXISTS selection_mode VARCHAR(20);

UPDATE collaboration_campaign
SET selection_mode = 'BRAND_DIRECT'
WHERE selection_mode IS NULL;

ALTER TABLE collaboration_campaign
    ALTER COLUMN selection_mode SET DEFAULT 'BRAND_DIRECT';

ALTER TABLE collaboration_campaign
    ALTER COLUMN selection_mode SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_campaign_selection_mode'
    ) THEN
        ALTER TABLE collaboration_campaign
            ADD CONSTRAINT ck_campaign_selection_mode
            CHECK (selection_mode IN ('BRAND_DIRECT', 'OPERATOR'));
    END IF;
END $$;

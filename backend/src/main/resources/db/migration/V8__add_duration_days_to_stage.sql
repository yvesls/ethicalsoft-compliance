DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables
		WHERE table_schema = 'public' AND table_name = 'stage'
	) THEN
		ALTER TABLE stage ADD COLUMN IF NOT EXISTS duration_days INTEGER;
	END IF;
END $$;


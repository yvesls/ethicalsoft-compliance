DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'questionnaire'
  ) THEN
    ALTER TABLE questionnaire
        ADD COLUMN IF NOT EXISTS domain      VARCHAR(50)  DEFAULT NULL,
        ADD COLUMN IF NOT EXISTS description VARCHAR(500) DEFAULT NULL;
  END IF;
END $$;


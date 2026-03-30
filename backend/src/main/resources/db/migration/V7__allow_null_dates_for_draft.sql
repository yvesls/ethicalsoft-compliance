DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'project'
    ) THEN
        ALTER TABLE project ALTER COLUMN start_date DROP NOT NULL;
        ALTER TABLE project ALTER COLUMN deadline DROP NOT NULL;
    END IF;
END $$;


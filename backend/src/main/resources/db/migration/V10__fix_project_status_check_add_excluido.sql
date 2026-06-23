DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'project'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'project_status_check'
              AND conrelid = 'project'::regclass
        ) THEN
            ALTER TABLE project DROP CONSTRAINT project_status_check;
        END IF;

        ALTER TABLE project ADD CONSTRAINT project_status_check
            CHECK (status IN ('ABERTO', 'CONCLUIDO', 'RASCUNHO', 'ARQUIVADO', 'EXCLUIDO'));
    END IF;
END $$;


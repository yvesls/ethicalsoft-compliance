DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'project_status_check'
          AND conrelid = 'project'::regclass
    ) THEN
        ALTER TABLE project DROP CONSTRAINT project_status_check;
    END IF;
END $$;

ALTER TABLE project ADD CONSTRAINT project_status_check
    CHECK (status IN ('ABERTO', 'CONCLUIDO', 'RASCUNHO', 'ARQUIVADO', 'EXCLUIDO'));


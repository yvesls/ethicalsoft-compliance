DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'project_status_check'
          AND table_name = 'project'
    ) THEN
        ALTER TABLE project DROP CONSTRAINT project_status_check;
    END IF;

    ALTER TABLE project ADD CONSTRAINT project_status_check
        CHECK (status IN ('ABERTO', 'CONCLUIDO', 'RASCUNHO', 'ARQUIVADO', 'EXCLUIDO'));
END $$;


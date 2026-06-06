DO $$
DECLARE
    _conname text;
BEGIN
    FOR _conname IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE rel.relname = 'project'
          AND nsp.nspname = 'public'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE project DROP CONSTRAINT %I', _conname);
    END LOOP;
END $$;

ALTER TABLE project ADD CONSTRAINT project_status_check
    CHECK (status IN ('ABERTO', 'CONCLUIDO', 'RASCUNHO', 'ARQUIVADO', 'EXCLUIDO'));


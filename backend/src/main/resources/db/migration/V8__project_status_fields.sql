ALTER TABLE project
    ADD COLUMN current_situation VARCHAR(100),
    ADD COLUMN timeline_status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';

ALTER TABLE stage
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN application_start_date DATE,
    ADD COLUMN application_end_date DATE;

ALTER TABLE iteration
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';

ALTER TABLE questionnaire
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';

UPDATE project SET current_situation = NULL, timeline_status = 'PENDENTE';
UPDATE stage SET status = 'PENDENTE';
UPDATE iteration SET status = 'PENDENTE';
UPDATE questionnaire SET status = 'PENDENTE';

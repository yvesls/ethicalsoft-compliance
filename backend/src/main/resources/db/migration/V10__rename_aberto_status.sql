UPDATE project SET timeline_status = 'EM_ANDAMENTO' WHERE timeline_status = 'ABERTO';
UPDATE stage SET status = 'EM_ANDAMENTO' WHERE status = 'ABERTO';
UPDATE iteration SET status = 'EM_ANDAMENTO' WHERE status = 'ABERTO';
UPDATE questionnaire SET status = 'EM_ANDAMENTO' WHERE status = 'ABERTO';


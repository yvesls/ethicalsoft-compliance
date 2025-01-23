-- Renomear tabelas
ALTER TABLE usuario RENAME TO user_account;
ALTER TABLE encargo RENAME TO role;
ALTER TABLE projeto RENAME TO project;
ALTER TABLE representante RENAME TO representative;
ALTER TABLE etapa RENAME TO stage;
ALTER TABLE questionario RENAME TO questionnaire;
ALTER TABLE questionario_etapa RENAME TO questionnaire_stage;
ALTER TABLE pergunta RENAME TO question;
ALTER TABLE pergunta_encargo RENAME TO question_role;

-- Renomear colunas da tabela user_account
ALTER TABLE user_account RENAME COLUMN usuario_id TO user_id;
ALTER TABLE user_account RENAME COLUMN nome TO first_name;
ALTER TABLE user_account RENAME COLUMN sobrenome TO last_name;
ALTER TABLE user_account RENAME COLUMN senha TO password;
ALTER TABLE user_account RENAME COLUMN aceito_termo TO accepted_terms;

-- Renomear colunas da tabela role
ALTER TABLE role RENAME COLUMN encargo_id TO role_id;
ALTER TABLE role RENAME COLUMN descricao TO description;

-- Renomear colunas da tabela template
ALTER TABLE template RENAME COLUMN nome TO name;
ALTER TABLE template RENAME COLUMN data_criacao TO creation_date;
ALTER TABLE template RENAME COLUMN projeto_id TO project_id;

-- Renomear colunas da tabela project
ALTER TABLE project RENAME COLUMN projeto_id TO project_id;
ALTER TABLE project RENAME COLUMN nome TO name;
ALTER TABLE project RENAME COLUMN tipo TO type;
ALTER TABLE project RENAME COLUMN data_inicio TO start_date;
ALTER TABLE project RENAME COLUMN prazo_limite TO deadline;
ALTER TABLE project RENAME COLUMN data_fechamento TO closing_date;
ALTER TABLE project RENAME COLUMN duracao_iteracoes TO iteration_duration;
ALTER TABLE project RENAME COLUMN num_iteracoes TO iteration_count;

-- Renomear colunas da tabela representative
ALTER TABLE representative RENAME COLUMN representante_id TO representative_id;
ALTER TABLE representative RENAME COLUMN projeto_id TO project_id;
ALTER TABLE representative RENAME COLUMN usuario_id TO user_id;
ALTER TABLE representative RENAME COLUMN encargo_id TO role_id;
ALTER TABLE representative RENAME COLUMN data_criacao TO creation_date;
ALTER TABLE representative RENAME COLUMN data_atualizacao TO update_date;
ALTER TABLE representative RENAME COLUMN data_exclusao TO deletion_date;

-- Renomear colunas da tabela stage
ALTER TABLE stage RENAME COLUMN etapa_id TO stage_id;
ALTER TABLE stage RENAME COLUMN projeto_id TO project_id;
ALTER TABLE stage RENAME COLUMN nome TO name;
ALTER TABLE stage RENAME COLUMN peso TO weight;

-- Renomear colunas da tabela questionnaire
ALTER TABLE questionnaire RENAME COLUMN questionario_id TO questionnaire_id;
ALTER TABLE questionnaire RENAME COLUMN projeto_id TO project_id;
ALTER TABLE questionnaire RENAME COLUMN nome TO name;
ALTER TABLE questionnaire RENAME COLUMN iteracao TO iteration;
ALTER TABLE questionnaire RENAME COLUMN peso TO weight;
ALTER TABLE questionnaire RENAME COLUMN faixa_aplicacao_inicio TO application_start_date;
ALTER TABLE questionnaire RENAME COLUMN faixa_aplicacao_fim TO application_end_date;

-- Renomear colunas da tabela questionnaire_stage
ALTER TABLE questionnaire_stage RENAME COLUMN questionario_id TO questionnaire_id;
ALTER TABLE questionnaire_stage RENAME COLUMN etapa_id TO stage_id;

-- Renomear colunas da tabela question
ALTER TABLE question RENAME COLUMN pergunta_id TO question_id;
ALTER TABLE question RENAME COLUMN questionario_id TO questionnaire_id;
ALTER TABLE question RENAME COLUMN texto TO text;

-- Renomear colunas da tabela question_role
ALTER TABLE question_role RENAME COLUMN pergunta_id TO question_id;
ALTER TABLE question_role RENAME COLUMN encargo_id TO role_id;

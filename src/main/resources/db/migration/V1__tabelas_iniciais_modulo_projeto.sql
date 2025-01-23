-- Tabela de Usuários
CREATE TABLE usuario (
    usuario_id SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    sobrenome VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(100) NOT NULL,
    aceito_termo BOOLEAN NOT NULL
);

-- Tabela de Encargos
CREATE TABLE encargo (
    encargo_id SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    descricao TEXT
);

-- Tabela de Templates
CREATE TABLE template (
    template_id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    data_criacao DATE
);

-- Tabela de Projetos
CREATE TABLE projeto (
     projeto_id SERIAL PRIMARY KEY,
     nome VARCHAR(100) NOT NULL,
     template_id INT REFERENCES template(template_id),
     tipo VARCHAR(50) NOT NULL,
     data_inicio DATE NOT NULL,
     prazo_limite DATE NOT NULL,
     status VARCHAR(20) NOT NULL,
     data_fechamento DATE,
     duracao_iteracoes INT,
     num_iteracoes INT
);

-- Alteração na tabela Template para inclusão de projeto_id
ALTER TABLE template
    ADD COLUMN projeto_id INT;
ALTER TABLE template
    ADD CONSTRAINT fk_template_projeto
        FOREIGN KEY (projeto_id)
            REFERENCES projeto(projeto_id);

-- Tabela de Representantes
CREATE TABLE representante (
    representante_id SERIAL PRIMARY KEY,
    projeto_id INT REFERENCES projeto(projeto_id),
    usuario_id INT REFERENCES usuario(usuario_id),
    encargo_id INT REFERENCES encargo(encargo_id),
    data_criacao DATE NOT NULL,
    data_atualizacao DATE,
    data_exclusao DATE
);

-- Tabela de Etapas
CREATE TABLE etapa (
    etapa_id SERIAL PRIMARY KEY,
    projeto_id INT REFERENCES projeto(projeto_id),
    nome VARCHAR(100) NOT NULL,
    peso INT NOT NULL
);

-- Tabela de Questionários
CREATE TABLE questionario (
    questionario_id SERIAL PRIMARY KEY,
    projeto_id INT REFERENCES projeto(projeto_id),
    nome VARCHAR(100) NOT NULL,
    iteracao VARCHAR(50),
    peso INT NOT NULL,
    faixa_aplicacao_inicio DATE,
    faixa_aplicacao_fim DATE
);

-- Tabela de Questionários Etapas
CREATE TABLE questionario_etapa (
    questionario_id INT REFERENCES questionario(questionario_id),
    etapa_id INT REFERENCES etapa(etapa_id),
    PRIMARY KEY (questionario_id, etapa_id)
);

-- Tabela de Perguntas
CREATE TABLE pergunta (
    pergunta_id SERIAL PRIMARY KEY,
    questionario_id INT REFERENCES questionario(questionario_id),
    texto TEXT NOT NULL
);

-- Tabela de Perguntas por Encargo
CREATE TABLE pergunta_encargo (
    pergunta_id INT REFERENCES pergunta(pergunta_id),
    encargo_id INT REFERENCES encargo(encargo_id),
    PRIMARY KEY (pergunta_id, encargo_id)
);

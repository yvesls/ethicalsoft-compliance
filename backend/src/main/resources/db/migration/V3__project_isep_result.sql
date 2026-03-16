CREATE TABLE IF NOT EXISTS project_isep_result (
    result_id      BIGSERIAL     PRIMARY KEY,
    project_id     BIGINT        NOT NULL UNIQUE,
    isep           NUMERIC(7, 4) NOT NULL,
    band           VARCHAR(1)    NOT NULL,
    questionnaire_count INT      NOT NULL DEFAULT 0,
    calculated_at  TIMESTAMP     NOT NULL,
    closed_by      VARCHAR(255),
    CONSTRAINT fk_pir_project FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE
);


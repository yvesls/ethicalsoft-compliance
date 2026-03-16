CREATE TABLE IF NOT EXISTS questionnaire_result (
    result_id        BIGSERIAL PRIMARY KEY,
    project_id       BIGINT        NOT NULL,
    questionnaire_id INT           NOT NULL,
    isep             NUMERIC(7, 4) NOT NULL,
    band             VARCHAR(1)    NOT NULL,
    team_simple_avg  NUMERIC(7, 4) NOT NULL,
    team_std_dev     NUMERIC(7, 4) NOT NULL,
    calculated_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_qr_project       FOREIGN KEY (project_id)       REFERENCES project(project_id),
    CONSTRAINT fk_qr_questionnaire FOREIGN KEY (questionnaire_id) REFERENCES questionnaire(questionnaire_id),
    CONSTRAINT uq_qr_questionnaire UNIQUE (questionnaire_id)
);

CREATE TABLE IF NOT EXISTS member_compliance_result (
    id                BIGSERIAL     PRIMARY KEY,
    result_id         BIGINT        NOT NULL,
    representative_id BIGINT        NOT NULL,
    icp               NUMERIC(7, 4) NOT NULL,
    band              VARCHAR(1)    NOT NULL,
    CONSTRAINT fk_mcr_result FOREIGN KEY (result_id) REFERENCES questionnaire_result(result_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS member_stage_compliance_result (
    id                BIGSERIAL     PRIMARY KEY,
    result_id         BIGINT        NOT NULL,
    representative_id BIGINT        NOT NULL,
    stage_id          INT           NOT NULL,
    iem               NUMERIC(7, 4) NOT NULL,
    CONSTRAINT fk_mscr_result FOREIGN KEY (result_id) REFERENCES questionnaire_result(result_id) ON DELETE CASCADE,
    CONSTRAINT fk_mscr_stage  FOREIGN KEY (stage_id)  REFERENCES stage(stage_id)
);


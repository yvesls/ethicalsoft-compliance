package com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult;

import java.util.List;
import java.util.Optional;

public interface IsepResultQueryPort {

    Optional<QuestionnaireResult> findByQuestionnaireId(Integer questionnaireId);

    List<QuestionnaireResult> findByProjectId(Long projectId);

    boolean existsByQuestionnaireId(Integer questionnaireId);
}


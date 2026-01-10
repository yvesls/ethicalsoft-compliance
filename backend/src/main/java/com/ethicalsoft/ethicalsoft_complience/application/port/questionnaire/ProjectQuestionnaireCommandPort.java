package com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.QuestionnaireDTO;

import java.util.Map;
import java.util.Set;

public interface ProjectQuestionnaireCommandPort {
    void createQuestionnaires(Project project,
                              Set<QuestionnaireDTO> questionnaireDTOs,
                              Map<String, Stage> stageMap,
                              Map<String, Iteration> iterationMap);
}


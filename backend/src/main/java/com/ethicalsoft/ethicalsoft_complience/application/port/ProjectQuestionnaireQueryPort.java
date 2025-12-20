package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectQuestionnaireSummaryDTO;

public interface ProjectQuestionnaireQueryPort {
    ProjectQuestionnaireSummaryDTO getProjectQuestionnaireSummary(Long projectId, Integer questionnaireId);
}


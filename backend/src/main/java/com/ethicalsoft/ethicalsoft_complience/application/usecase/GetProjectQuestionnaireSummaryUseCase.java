package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectQuestionnaireSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectQuestionnaireSummaryUseCase {

    private final ProjectQuestionnaireQueryPort projectQuestionnaireQueryPort;

    public ProjectQuestionnaireSummaryDTO execute(Long projectId, Integer questionnaireId) {
        return projectQuestionnaireQueryPort.getProjectQuestionnaireSummary(projectId, questionnaireId);
    }
}

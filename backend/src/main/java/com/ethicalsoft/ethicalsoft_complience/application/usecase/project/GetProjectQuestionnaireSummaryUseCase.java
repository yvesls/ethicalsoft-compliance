package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectQuestionnaireSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQuestionnaireQueryPort;
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

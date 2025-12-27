package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQuestionnaireQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectQuestionnairesUseCase {

    private final ProjectQuestionnaireQueryPort projectQuestionnaireQueryPort;

    public Page<QuestionnaireSummaryResponseDTO> execute(Long projectId,
                                                         Pageable pageable,
                                                         QuestionnaireSearchFilter filter) {
        return projectQuestionnaireQueryPort.listQuestionnaires(projectId, pageable, filter);
    }
}

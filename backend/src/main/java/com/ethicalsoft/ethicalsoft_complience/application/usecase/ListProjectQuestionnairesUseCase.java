package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQueryPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectQuestionnairesUseCase {

    private final ProjectQueryPort projectQueryPort;

    public Page<QuestionnaireSummaryResponseDTO> execute(Long projectId,
                                                         Pageable pageable,
                                                         QuestionnaireSearchFilter filter) {
        return projectQueryPort.listQuestionnaires(projectId, pageable, filter);
    }
}

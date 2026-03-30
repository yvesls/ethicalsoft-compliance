package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.ProjectQuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.QuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.RepresentativeAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListProjectQuestionnairesUseCase {

    private final ProjectQuestionnaireQueryPort projectQuestionnaireQueryPort;
    private final RepresentativeAccessPolicy representativeAccessPolicy;
    private final QuestionnaireQueryPort questionnaireQueryPort;

    public Page<QuestionnaireSummaryResponseDTO> execute(Long projectId,
                                                         Pageable pageable,
                                                         QuestionnaireSearchFilter filter) {
        Long representativeId = representativeAccessPolicy.resolveRepresentativeIdForResponse(projectId);
        List<Long> roleIds = null;
        if (representativeId != null) {
            roleIds = questionnaireQueryPort.findRepresentativeRoleIds(projectId, representativeId);
        }
        return projectQuestionnaireQueryPort.listQuestionnaires(projectId, pageable, filter, roleIds);
    }
}

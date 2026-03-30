package com.ethicalsoft.ethicalsoft_complience.application.port.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectQuestionnaireSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectQuestionnaireQueryPort {
    ProjectQuestionnaireSummaryDTO getProjectQuestionnaireSummary(Long projectId, Integer questionnaireId);

    default Page<QuestionnaireSummaryResponseDTO> listQuestionnaires(Long projectId, Pageable pageable, QuestionnaireSearchFilter filter) {
        return listQuestionnaires(projectId, pageable, filter, null);
    }

    Page<QuestionnaireSummaryResponseDTO> listQuestionnaires(Long projectId, Pageable pageable, QuestionnaireSearchFilter filter, List<Long> representativeRoleIds);
}

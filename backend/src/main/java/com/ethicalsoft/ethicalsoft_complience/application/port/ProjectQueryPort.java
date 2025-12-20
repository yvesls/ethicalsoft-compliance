package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectDetailResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectQueryPort {
    Page<ProjectSummaryResponseDTO> search(ProjectSearchRequestDTO filters, Pageable pageable);
    ProjectDetailResponseDTO getProjectDetail(Long projectId);
    Page<QuestionnaireSummaryResponseDTO> listQuestionnaires(Long projectId, Pageable pageable, QuestionnaireSearchFilter filter);
}

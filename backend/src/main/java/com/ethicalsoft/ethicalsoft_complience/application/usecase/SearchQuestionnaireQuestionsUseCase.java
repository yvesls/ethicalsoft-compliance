package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.QuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.RepresentativeAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchQuestionnaireQuestionsUseCase {

    private final QuestionnaireQueryPort questionnaireQueryPort;
    private final RepresentativeAccessPolicy representativeAccessPolicy;

    public Page<QuestionnaireQuestionResponseDTO> execute(Long projectId,
                                                          Integer questionnaireId,
                                                          QuestionSearchFilterDTO filter,
                                                          Pageable pageable) {
        Long representativeId = representativeAccessPolicy.resolveRepresentativeIdForResponse(projectId);
        if (representativeId != null) {
            List<Long> roleIds = questionnaireQueryPort.findRepresentativeRoleIds(projectId, representativeId);
            if (filter == null) {
                filter = new QuestionSearchFilterDTO();
            }
            if (filter.getRoleIds() == null || filter.getRoleIds().isEmpty()) {
                filter.setRoleIds(roleIds);
            }
        }
        return questionnaireQueryPort.searchQuestions(questionnaireId, filter, pageable);
    }
}

package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.QuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.RepresentativeAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListQuestionnaireQuestionsUseCase {

    private final QuestionnaireQueryPort questionnaireQueryPort;
    private final RepresentativeAccessPolicy representativeAccessPolicy;


    public Page<QuestionnaireQuestionResponseDTO> executeForRepresentative(Long projectId,
                                                                            Integer questionnaireId,
                                                                            Pageable pageable,
                                                                            String questionText,
                                                                            String roleName,
                                                                            Long representativeId) {
        QuestionSearchFilterDTO filter = new QuestionSearchFilterDTO();
        filter.setQuestionText(questionText);
        filter.setRoleName(roleName);

        Long effectiveRepresentativeId = representativeId;
        if (effectiveRepresentativeId == null && projectId != null) {
            effectiveRepresentativeId = representativeAccessPolicy.resolveRepresentativeIdForResponse(projectId);
        }
        filter.setRoleIds(effectiveRepresentativeId != null
                ? questionnaireQueryPort.findRepresentativeRoleIds(projectId, effectiveRepresentativeId)
                : null);

        return questionnaireQueryPort.searchQuestions(questionnaireId, filter, pageable);
    }

    public Page<QuestionnaireQuestionResponseDTO> executeForAdmin(Long projectId,
                                                                   Integer questionnaireId,
                                                                   Pageable pageable,
                                                                   String questionText,
                                                                   String roleName) {
        if (projectId != null && !representativeAccessPolicy.isAdminOrOwner(projectId)) {
            throw new com.ethicalsoft.ethicalsoft_complience.exception.BusinessException(
                    "Acesso negado: apenas administradores ou donos do projeto podem listar todas as perguntas.");
        }

        QuestionSearchFilterDTO filter = new QuestionSearchFilterDTO();
        filter.setQuestionText(questionText);
        filter.setRoleName(roleName);
        filter.setRoleIds(null);

        return questionnaireQueryPort.searchQuestions(questionnaireId, filter, pageable);
    }

}

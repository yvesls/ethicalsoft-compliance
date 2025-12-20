package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.service.QuestionnaireQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchQuestionnaireQuestionsUseCase {

    private final QuestionnaireQueryService questionnaireQueryService;

    public Page<QuestionnaireQuestionResponseDTO> execute(Long projectId,
                                                           Integer questionnaireId,
                                                           QuestionSearchFilterDTO filter,
                                                           Pageable pageable) {
        return questionnaireQueryService.searchQuestions(projectId, questionnaireId, filter, pageable);
    }
}

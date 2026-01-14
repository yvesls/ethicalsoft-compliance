package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.QuestionnaireQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchQuestionnaireQuestionsUseCase {

    private final QuestionnaireQueryPort questionnaireQueryPort;

    public Page<QuestionnaireQuestionResponseDTO> execute(Integer questionnaireId,
                                                          QuestionSearchFilterDTO filter,
                                                          Pageable pageable) {
        return questionnaireQueryPort.searchQuestions(questionnaireId, filter, pageable);
    }
}

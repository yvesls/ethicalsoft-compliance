package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListQuestionnaireQuestionsUseCase {

    private final QuestionnaireQueryPort questionnaireQueryPort;

    public Page<QuestionnaireQuestionResponseDTO> execute(Integer questionnaireId,
                                                          Pageable pageable,
                                                          String questionText,
                                                          String roleName) {
        QuestionSearchFilterDTO filter = new QuestionSearchFilterDTO();
        filter.setQuestionText(questionText);
        filter.setRoleName(roleName);
        return questionnaireQueryPort.searchQuestions(questionnaireId, filter, pageable);
    }
}

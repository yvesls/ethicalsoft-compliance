package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireResponsePort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireAnswerPageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetQuestionnaireAnswersPageUseCase {

    private final QuestionnaireResponsePort questionnaireResponsePort;

    public QuestionnaireAnswerPageResponseDTO execute(Long projectId, Integer questionnaireId, Pageable pageable) {
        return questionnaireResponsePort.getAnswerPage(projectId, questionnaireId, pageable);
    }
}

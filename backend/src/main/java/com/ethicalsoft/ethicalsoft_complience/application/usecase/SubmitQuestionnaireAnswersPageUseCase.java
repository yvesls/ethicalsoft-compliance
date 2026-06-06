package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireAnswersRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireAnswersResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.QuestionnaireResponsePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmitQuestionnaireAnswersPageUseCase {

    private final QuestionnaireResponsePort questionnaireResponsePort;

    public QuestionnaireAnswersResponseDTO execute(Long projectId,
                                                   Integer questionnaireId,
                                                   QuestionnaireAnswersRequestDTO request) {
        return questionnaireResponsePort.submitAnswers(projectId, questionnaireId, request);
    }
}

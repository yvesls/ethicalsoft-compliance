package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireResponsePort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireAnswerPageRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireAnswerPageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmitQuestionnaireAnswersPageUseCase {

    private final QuestionnaireResponsePort questionnaireResponsePort;

    public QuestionnaireAnswerPageResponseDTO execute(Long projectId,
                                                      Integer questionnaireId,
                                                      QuestionnaireAnswerPageRequestDTO request) {
        return questionnaireResponsePort.submitAnswerPage(projectId, questionnaireId, request);
    }
}

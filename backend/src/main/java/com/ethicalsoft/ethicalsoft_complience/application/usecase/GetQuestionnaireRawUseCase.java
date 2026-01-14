package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireRawResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.QuestionnaireQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetQuestionnaireRawUseCase {

    private final QuestionnaireQueryPort questionnaireQueryPort;

    public QuestionnaireRawResponseDTO execute(Long projectId, Integer questionnaireId) {
        return questionnaireQueryPort.getQuestionnaireRaw(projectId, questionnaireId);
    }
}

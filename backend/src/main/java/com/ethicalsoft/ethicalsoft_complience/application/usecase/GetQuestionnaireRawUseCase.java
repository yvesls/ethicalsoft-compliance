package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireQueryPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireRawResponseDTO;
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

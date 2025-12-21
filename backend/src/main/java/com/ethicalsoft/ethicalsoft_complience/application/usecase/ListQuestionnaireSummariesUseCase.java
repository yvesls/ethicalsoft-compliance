package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireResponsePort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireResponseSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListQuestionnaireSummariesUseCase {

    private final QuestionnaireResponsePort questionnaireResponsePort;

    public List<QuestionnaireResponseSummaryDTO> execute(Long projectId, Integer questionnaireId) {
        return questionnaireResponsePort.listSummaries(projectId, questionnaireId);
    }
}

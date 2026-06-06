package com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireAnswersRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireAnswersResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireResponseSummaryDTO;

import java.util.List;

public interface QuestionnaireResponsePort {

    QuestionnaireAnswersResponseDTO getAnswers(Long projectId, Integer questionnaireId);

    QuestionnaireAnswersResponseDTO submitAnswers(Long projectId, Integer questionnaireId, QuestionnaireAnswersRequestDTO request);

    List<QuestionnaireResponseSummaryDTO> listSummaries(Long projectId, Integer questionnaireId);
}

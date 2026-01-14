package com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireAnswerPageRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireAnswerPageResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireResponseSummaryDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QuestionnaireResponsePort {

    QuestionnaireAnswerPageResponseDTO getAnswerPage(Long projectId, Integer questionnaireId, Pageable pageable);

    QuestionnaireAnswerPageResponseDTO submitAnswerPage(Long projectId, Integer questionnaireId, QuestionnaireAnswerPageRequestDTO request);

    List<QuestionnaireResponseSummaryDTO> listSummaries(Long projectId, Integer questionnaireId);
}

package com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireRawResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionnaireQueryPort {
    QuestionnaireRawResponseDTO getQuestionnaireRaw(Long projectId, Integer questionnaireId);
    Page<QuestionnaireQuestionResponseDTO> searchQuestions(Integer questionnaireId, QuestionSearchFilterDTO filter, Pageable pageable);
}

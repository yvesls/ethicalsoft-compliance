package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireRawResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionnaireQueryPort {
    QuestionnaireRawResponseDTO getQuestionnaireRaw(Long projectId, Integer questionnaireId);
    Page<QuestionnaireQuestionResponseDTO> searchQuestions(Long projectId, Integer questionnaireId, QuestionSearchFilterDTO filter, Pageable pageable);
}


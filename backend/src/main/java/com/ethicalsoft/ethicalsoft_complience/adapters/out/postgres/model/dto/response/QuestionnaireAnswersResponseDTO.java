package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record QuestionnaireAnswersResponseDTO(boolean completed, List<QuestionnaireAnswerResponseDTO> answers) {
}


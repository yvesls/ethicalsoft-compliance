package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QuestionnaireAnswersResponseDTO {
    boolean completed;
    List<QuestionnaireAnswerResponseDTO> answers;
}


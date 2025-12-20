package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QuestionnaireAnswerPageResponseDTO {
    Integer pageNumber;
    Integer pageSize;
    Integer totalPages;
    boolean completed;
    List<QuestionnaireAnswerResponseDTO> answers;
}


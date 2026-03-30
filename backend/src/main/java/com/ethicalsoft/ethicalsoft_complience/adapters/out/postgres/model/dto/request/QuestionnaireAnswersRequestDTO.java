package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionnaireAnswersRequestDTO {

    private Long representativeId;

    @Valid
    @NotNull
    private List<QuestionnaireAnswerRequestDTO> answers;

    private boolean draft;
}


package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionnaireAnswerPageRequestDTO {

    @NotNull
    @Min(0)
    private Integer pageNumber;

    @NotNull
    @Min(1)
    private Integer pageSize;

    @Valid
    @NotNull
    private List<QuestionnaireAnswerRequestDTO> answers;
}

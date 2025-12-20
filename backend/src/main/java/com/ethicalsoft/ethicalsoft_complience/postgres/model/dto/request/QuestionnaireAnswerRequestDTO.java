package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionnaireAnswerRequestDTO {
    @NotNull
    private Long questionId;

    @NotNull
    private Boolean response;

    private LinkDTO justification;

    private LinkDTO evidence;

    private List<LinkDTO> attachments;
}

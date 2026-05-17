package com.ethicalsoft.ethicalsoft_complience.controller.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskQuestionRequestDTO(
        @NotBlank(message = "A pergunta não pode estar vazia.")
        @Size(max = 500, message = "A pergunta deve ter no máximo 500 caracteres.")
        String question
) {}


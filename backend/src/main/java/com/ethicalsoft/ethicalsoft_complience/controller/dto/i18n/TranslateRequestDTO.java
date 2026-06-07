package com.ethicalsoft.ethicalsoft_complience.controller.dto.i18n;

import jakarta.validation.constraints.NotBlank;

public record TranslateRequestDTO(
        @NotBlank(message = "O texto é obrigatório.") String text,
        @NotBlank(message = "O idioma é obrigatório.") String language
) {}

package com.ethicalsoft.ethicalsoft_complience.controller.dto.i18n;

import jakarta.validation.constraints.NotBlank;

public record TranslateRequestDTO(
        @NotBlank String text,
        @NotBlank String language
) {
}

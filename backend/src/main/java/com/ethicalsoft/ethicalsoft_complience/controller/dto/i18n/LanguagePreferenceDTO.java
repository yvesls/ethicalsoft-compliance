package com.ethicalsoft.ethicalsoft_complience.controller.dto.i18n;

import jakarta.validation.constraints.NotBlank;

public record LanguagePreferenceDTO(
        @NotBlank(message = "O idioma é obrigatório.")
        String language
) {}

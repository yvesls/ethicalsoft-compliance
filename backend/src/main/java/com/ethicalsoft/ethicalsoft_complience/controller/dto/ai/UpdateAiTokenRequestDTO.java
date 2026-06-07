package com.ethicalsoft.ethicalsoft_complience.controller.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAiTokenRequestDTO(
        @NotBlank(message = "O token de IA não pode estar vazio.")
        @Size(min = 10, max = 512, message = "Tamanho de token inválido.")
        String token
) {
}

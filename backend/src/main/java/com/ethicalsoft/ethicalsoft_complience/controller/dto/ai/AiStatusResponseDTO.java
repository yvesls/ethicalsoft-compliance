package com.ethicalsoft.ethicalsoft_complience.controller.dto.ai;

public record AiStatusResponseDTO(
        boolean enabled,
        String provider,
        String model,
        boolean available,
        boolean userTokenConfigured
) {}

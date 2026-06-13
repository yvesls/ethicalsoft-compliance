package com.ethicalsoft.ethicalsoft_complience.controller.dto.ai;

import java.time.LocalDateTime;

public record AiTokenStatusDTO(
        boolean configured,
        String provider,
        String tokenHint,
        LocalDateTime updatedAt
) {
    public static AiTokenStatusDTO notConfigured() {
        return new AiTokenStatusDTO(false, "groq-cloud", null, null);
    }
}

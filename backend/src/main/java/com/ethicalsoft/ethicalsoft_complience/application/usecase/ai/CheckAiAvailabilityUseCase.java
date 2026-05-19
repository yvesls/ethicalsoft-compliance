package com.ethicalsoft.ethicalsoft_complience.application.usecase.ai;

import com.ethicalsoft.ethicalsoft_complience.application.port.ai.LlmAnalysisPort;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AiStatusResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.infra.config.AiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckAiAvailabilityUseCase {

    private final LlmAnalysisPort llmAnalysisPort;
    private final AiConfig aiConfig;

    public AiStatusResponseDTO execute() {
        boolean available = aiConfig.isEnabled() && llmAnalysisPort.isAvailable();
        return new AiStatusResponseDTO(aiConfig.isEnabled(), "groq-cloud", aiConfig.getModelName(), available);
    }
}

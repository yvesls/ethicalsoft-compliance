package com.ethicalsoft.ethicalsoft_complience.application.usecase.ai;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserAiTokenRepository;
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
    private final UserAiTokenRepository userAiTokenRepository;

    public AiStatusResponseDTO execute(Long userId) {
        boolean engineAvailable = aiConfig.isEnabled() && llmAnalysisPort.isAvailable();
        boolean tokenConfigured = userId != null && safeHasToken(userId);
        boolean available = engineAvailable && tokenConfigured;
        return new AiStatusResponseDTO(
                aiConfig.isEnabled(),
                "groq-cloud",
                aiConfig.getModelName(),
                available,
                tokenConfigured
        );
    }

    private boolean safeHasToken(Long userId) {
        try {
            return userAiTokenRepository.existsByUserId(userId);
        } catch (Exception ex) {
            return false;
        }
    }
}

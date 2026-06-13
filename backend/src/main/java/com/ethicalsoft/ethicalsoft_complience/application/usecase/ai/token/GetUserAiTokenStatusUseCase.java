package com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.token;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.UserAiTokenDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserAiTokenRepository;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AiTokenStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetUserAiTokenStatusUseCase {

    private final UserAiTokenRepository repository;

    public AiTokenStatusDTO execute(Long userId) {
        Optional<UserAiTokenDocument> token = repository.findByUserId(userId);
        return token
                .map(doc -> new AiTokenStatusDTO(
                        true,
                        doc.getProvider() != null ? doc.getProvider() : "groq-cloud",
                        doc.getTokenHint(),
                        doc.getUpdatedAt()))
                .orElseGet(AiTokenStatusDTO::notConfigured);
    }
}

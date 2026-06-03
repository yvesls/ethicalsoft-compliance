package com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.token;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.UserAiTokenDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserAiTokenRepository;
import com.ethicalsoft.ethicalsoft_complience.application.service.ai.AiTokenEncryptionService;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AiTokenStatusDTO;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserAiTokenUseCase {

    private final UserAiTokenRepository repository;
    private final AiTokenEncryptionService encryption;

    public AiTokenStatusDTO execute(Long userId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("ai.token.empty", "Informe um token de IA válido.");
        }
        String token = rawToken.trim();

        String encrypted = encryption.encrypt(token);
        String hint = encryption.maskToken(token);
        LocalDateTime now = LocalDateTime.now();

        Optional<UserAiTokenDocument> existing = repository.findByUserId(userId);
        UserAiTokenDocument doc = existing.orElseGet(() ->
                UserAiTokenDocument.builder()
                        .userId(userId)
                        .createdAt(now)
                        .build());

        doc.setEncryptedToken(encrypted);
        doc.setTokenHint(hint);
        doc.setProvider("groq-cloud");
        doc.setUpdatedAt(now);
        if (doc.getCreatedAt() == null) {
            doc.setCreatedAt(now);
        }

        repository.save(doc);
        log.info("[ai-token] Token de IA atualizado para userId={}", userId);

        return new AiTokenStatusDTO(true, doc.getProvider(), hint, now);
    }
}

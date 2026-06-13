package com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.token;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserAiTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteUserAiTokenUseCase {

    private final UserAiTokenRepository repository;

    public void execute(Long userId) {
        try {
            repository.deleteByUserId(userId);
            log.info("[ai-token] Token de IA removido para userId={}", userId);
        } catch (RuntimeException ex) {
            log.warn("[ai-token] Falha ao remover token de IA userId={}: {}", userId, ex.getMessage());
        }
    }
}

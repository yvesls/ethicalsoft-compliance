package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.UserAiTokenDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserAiTokenRepository;
import com.ethicalsoft.ethicalsoft_complience.application.service.ai.AiTokenEncryptionService;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.infra.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserChatModelResolver {

    private final UserAiTokenRepository tokenRepository;
    private final AiTokenEncryptionService encryption;
    private final AiConfig aiConfig;

    private final Map<String, ChatModel> cache = new ConcurrentHashMap<>();

    public ChatModel resolveRequired(Long userId) {
        return resolve(userId).orElseThrow(() -> new BusinessException(
                "Token de IA não configurado. Cadastre seu token nas configurações para utilizar este recurso."
        ));
    }

    public Optional<ChatModel> resolve(Long userId) {
        if (userId == null) return Optional.empty();
        try {
            return tokenRepository.findByUserId(userId)
                    .map(this::buildFromDocument);
        } catch (Exception ex) {
            log.warn("[ai-resolver] Falha ao resolver token de IA userId={}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean hasToken(Long userId) {
        if (userId == null) return false;
        try {
            return tokenRepository.existsByUserId(userId);
        } catch (Exception ex) {
            return false;
        }
    }

    public void invalidate(Long userId) {
        if (userId == null) return;
        cache.keySet().removeIf(key -> key.startsWith(userId + "::"));
    }

    private ChatModel buildFromDocument(UserAiTokenDocument doc) {
        String plain = encryption.decrypt(doc.getEncryptedToken());
        if (plain == null || plain.isBlank()) {
            throw new BusinessException("Não foi possível ler seu token de IA. Cadastre-o novamente.");
        }
        String cacheKey = doc.getUserId() + "::" + Integer.toHexString(plain.hashCode());
        return cache.computeIfAbsent(cacheKey, k -> build(plain));
    }

    private ChatModel build(String apiKey) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(aiConfig.getBaseUrl())
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiConfig.getModelName())
                .temperature(aiConfig.getTemperature())
                .maxTokens(aiConfig.getMaxTokens())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }
}

package com.ethicalsoft.ethicalsoft_complience.application.usecase.i18n;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.TranslationCacheDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.TranslationCacheRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.ai.LlmAnalysisPort;
import com.ethicalsoft.ethicalsoft_complience.domain.i18n.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslateDynamicTextUseCase {

    private final TranslationCacheRepository cacheRepository;
    private final LlmAnalysisPort llmAnalysisPort;

    public String execute(String text, String targetLanguageCode) {
        if (text == null || text.isBlank()) {
            return text;
        }
        SupportedLanguage target = SupportedLanguage.fromCodeOrDefault(targetLanguageCode);
        if (target == SupportedLanguage.DEFAULT) {
            return text;
        }

        String hash = computeHash(text, target.code());
        Optional<TranslationCacheDocument> cached = safeFindByHash(hash);
        if (cached.isPresent()) {
            return cached.get().getTranslated();
        }

        String translated = llmAnalysisPort.translate(text, target.code());
        if (translated == null || translated.isBlank()) {
            return text;
        }
        if (translated.equals(text)) {
            return text;
        }

        safeSave(TranslationCacheDocument.builder()
                .hash(hash)
                .language(target.code())
                .original(text)
                .translated(translated)
                .createdAt(LocalDateTime.now())
                .build());

        return translated;
    }

    private Optional<TranslationCacheDocument> safeFindByHash(String hash) {
        try {
            return cacheRepository.findByHash(hash);
        } catch (Exception e) {
            log.warn("[i18n-cache] Falha ao consultar cache de tradução: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void safeSave(@NonNull TranslationCacheDocument document) {
        try {
            cacheRepository.save(document);
        } catch (Exception e) {
            log.warn("[i18n-cache] Falha ao salvar tradução em cache (hash={}): {}",
                    document.getHash(), e.getMessage());
        }
    }

    private String computeHash(String text, String languageCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((text + "|" + languageCode).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            return Integer.toHexString((text + "|" + languageCode).hashCode());
        }
    }
}

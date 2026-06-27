package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.AiGenerationCacheDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.AiGenerationCacheRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.ai.LlmAnalysisPort;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.infra.config.AiConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class GroqLlmAdapter implements LlmAnalysisPort {

    private static final String OP_INSIGHTS = "INSIGHTS";
    private static final String OP_RISK_REPORT = "RISK_REPORT";
    private static final String OP_EXPLAIN_ISEP = "EXPLAIN_ISEP";
    private static final String DEFAULT_LANGUAGE = "pt-BR";
    private static final String UNAVAILABLE_MESSAGE =
            "Análise de IA temporariamente indisponível. Tente novamente.";

    private final UserChatModelResolver chatModelResolver;
    private final AiDataSanitizer sanitizer;
    private final AiPromptBuilder promptBuilder;
    private final AiConfig aiConfig;
    private final AiGenerationCacheRepository generationCacheRepository;

    @Override
    @Async
    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackInsightsLang")
    @Retry(name = "llm")
    public CompletableFuture<AiInsightResult> generateInsights(DashboardSnapshot snapshot,
                                                               String language, Long userId) {
        return CompletableFuture.completedFuture(
                generateOrCache(OP_INSIGHTS, snapshot, language, userId,
                        () -> promptBuilder.buildInsightsPrompt(sanitizer.sanitize(snapshot), language))
        );
    }

    @Override
    @Async
    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackInsightsLang")
    @Retry(name = "llm")
    public CompletableFuture<AiInsightResult> generateRiskReport(DashboardSnapshot snapshot,
                                                                 String language, Long userId) {
        return CompletableFuture.completedFuture(
                generateOrCache(OP_RISK_REPORT, snapshot, language, userId,
                        () -> promptBuilder.buildRiskReportPrompt(sanitizer.sanitize(snapshot), language))
        );
    }

    @Override
    @Async
    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackInsightsLang")
    @Retry(name = "llm")
    public CompletableFuture<AiInsightResult> explainIsepResults(DashboardSnapshot snapshot,
                                                                 String language, Long userId) {
        return CompletableFuture.completedFuture(
                generateOrCache(OP_EXPLAIN_ISEP, snapshot, language, userId,
                        () -> promptBuilder.buildExplainIsepPrompt(sanitizer.sanitize(snapshot), language))
        );
    }

    @Override
    public void askQuestion(String question, DashboardSnapshot snapshot, SseEmitter emitter,
                            String language, Long userId) {
        log.info("[llm-groq] Pergunta Q&A do usuário ({}, userId={}): '{}'", language, userId, question);

        DashboardSnapshot sanitized = sanitizer.sanitize(snapshot);
        Prompt prompt = promptBuilder.buildQaPrompt(question, sanitized, language);

        try {
            ChatModel chatModel = chatModelResolver.resolveRequired(userId);
            ChatResponse response = chatModel.call(prompt);
            String fullText = response.getResult().getOutput().getText();

            if (fullText != null && !fullText.isEmpty()) {
                String[] words = fullText.split("(?<=\\s)");
                for (String word : words) {
                    emitter.send(SseEmitter.event().data(word));
                }
            }
            emitter.complete();
            log.info("[llm-groq] Resposta Q&A enviada com sucesso");
        } catch (Exception e) {
            log.error("[llm-groq] Erro na chamada Q&A ao LLM", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(friendlyMessage(e)));
            } catch (Exception sendError) {
                log.debug("[llm-groq] Falha ao enviar evento de erro Q&A: {}", sendError.getMessage());
            }
            emitter.complete();
        }
    }

    @Override
    public String translate(String text, String targetLanguage, Long userId) {
        if (text == null || text.isBlank() || targetLanguage == null || targetLanguage.isBlank()) {
            return text;
        }
        Optional<ChatModel> chatModel = chatModelResolver.resolve(userId);
        if (chatModel.isEmpty()) {
            return text;
        }
        try {
            String system = """
                    Você é um tradutor profissional. Sua única tarefa é traduzir o texto fornecido
                    para o idioma indicado, preservando exatamente o significado, o tom e a formatação
                    Markdown. Retorne apenas o texto traduzido, sem comentários, sem aspas ao redor
                    e sem explicações adicionais.
                    """;
            String user = "Idioma de destino: " + targetLanguage + "\n\nTexto original:\n" + text;
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(system),
                    new UserMessage(user)
            ));
            ChatResponse response = chatModel.get().call(prompt);
            String translated = response.getResult().getOutput().getText();
            return (translated == null || translated.isBlank()) ? text : translated.trim();
        } catch (RuntimeException e) {
            log.warn("[llm-groq] Falha na tradução para {} (userId={}): {}",
                    targetLanguage, userId, e.getMessage());
            return text;
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isAvailableForUser(Long userId) {
        return isAvailable() && chatModelResolver.hasToken(userId);
    }

    private AiInsightResult generateOrCache(String operation, DashboardSnapshot snapshot,
                                            String language, Long userId, Supplier<Prompt> promptSupplier) {
        String normalizedLang = normalizeLanguage(language);
        String cacheKey = cacheHash(operation, snapshot, normalizedLang);

        Optional<AiGenerationCacheDocument> cached = safeFindCached(cacheKey);
        if (cached.isPresent()) {
            log.info("[llm-groq] Cache HIT operação={} projeto={} questionário={} idioma={}",
                    operation, snapshot.getProjectId(), snapshot.getQuestionnaireId(), normalizedLang);
            return AiInsightResult.success(cached.get().getContent(), aiConfig.getModelName());
        }

        log.info("[llm-groq] Cache MISS operação={} projeto={} questionário={} idioma={} userId={}",
                operation, snapshot.getProjectId(), snapshot.getQuestionnaireId(), normalizedLang, userId);

        ChatModel chatModel = chatModelResolver.resolveRequired(userId);
        ChatResponse response = chatModel.call(promptSupplier.get());
        String content = response.getResult().getOutput().getText();

        if (content != null && !content.isBlank() && cacheKey != null) {
            safeSaveCached(AiGenerationCacheDocument.builder()
                    .hash(cacheKey)
                    .operation(operation)
                    .language(normalizedLang)
                    .projectId(snapshot.getProjectId())
                    .questionnaireId(snapshot.getQuestionnaireId())
                    .content(content)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                    .build());
        }

        return AiInsightResult.success(content, aiConfig.getModelName());
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        String first = language.split(",")[0].trim();
        return first.isBlank() ? DEFAULT_LANGUAGE : first;
    }

    private String cacheHash(String operation, DashboardSnapshot snapshot, String language) {
        if (snapshot == null || snapshot.getIsepPercent() == null) {
            return null;
        }
        String raw = operation + "|" + snapshot.getProjectId()
                + "|" + snapshot.getQuestionnaireId()
                + "|" + snapshot.getIsepPercent()
                + "|" + snapshot.getBand()
                + "|" + language;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private Optional<AiGenerationCacheDocument> safeFindCached(String hash) {
        if (hash == null) {
            return Optional.empty();
        }
        try {
            return generationCacheRepository.findByHash(hash);
        } catch (RuntimeException ex) {
            log.warn("[llm-groq] Falha ao consultar cache de IA: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private void safeSaveCached(AiGenerationCacheDocument document) {
        try {
            generationCacheRepository.save(document);
        } catch (RuntimeException ex) {
            log.warn("[llm-groq] Falha ao salvar resposta de IA no cache (hash={}): {}",
                    document.getHash(), ex.getMessage());
        }
    }

    private String friendlyMessage(Exception e) {
        if (e instanceof BusinessException be) {
            return be.getMessage();
        }
        return UNAVAILABLE_MESSAGE;
    }

    @SuppressWarnings("unused")
    private CompletableFuture<AiInsightResult> fallbackInsightsLang(
            DashboardSnapshot snapshot, String language, Long userId, Throwable t) {
        log.warn("[llm-groq] Fallback ativado para projeto={} idioma={} userId={}: {}",
                snapshot.getProjectId(), language, userId, t.getMessage());
        String fallback = (t instanceof BusinessException be)
                ? be.getMessage()
                : "Análise de IA temporariamente indisponível. Os dados do dashboard continuam " +
                  "disponíveis normalmente. Tente novamente em alguns instantes.";
        return CompletableFuture.completedFuture(AiInsightResult.unavailable(fallback));
    }
}

package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import com.ethicalsoft.ethicalsoft_complience.application.port.ai.LlmAnalysisPort;
import com.ethicalsoft.ethicalsoft_complience.infra.config.AiConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class GroqLlmAdapter implements LlmAnalysisPort {

    private final ChatModel chatModel;
    private final AiDataSanitizer sanitizer;
    private final AiPromptBuilder promptBuilder;
    private final AiConfig aiConfig;

    @Override
    @Async
    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackInsights")
    @Retry(name = "llm")
    public CompletableFuture<AiInsightResult> generateInsights(DashboardSnapshot snapshot) {
        log.info("[llm-groq] Gerando insights para projeto={} questionário={}",
                snapshot.getProjectId(), snapshot.getQuestionnaireId());

        DashboardSnapshot sanitized = sanitizer.sanitize(snapshot);
        Prompt prompt = promptBuilder.buildInsightsPrompt(sanitized);

        ChatResponse response = chatModel.call(prompt);
        String content = response.getResult().getOutput().getText();

        log.info("[llm-groq] Insights gerados com sucesso para questionário={}",
                snapshot.getQuestionnaireId());
        return CompletableFuture.completedFuture(AiInsightResult.success(content, aiConfig.getModelName()));
    }

    @Override
    @Async
    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackInsights")
    @Retry(name = "llm")
    public CompletableFuture<AiInsightResult> generateRiskReport(DashboardSnapshot snapshot) {
        log.info("[llm-groq] Gerando relatório de risco para projeto={}", snapshot.getProjectId());

        DashboardSnapshot sanitized = sanitizer.sanitize(snapshot);
        Prompt prompt = promptBuilder.buildRiskReportPrompt(sanitized);

        ChatResponse response = chatModel.call(prompt);
        String content = response.getResult().getOutput().getText();

        log.info("[llm-groq] Relatório de risco gerado para projeto={}", snapshot.getProjectId());
        return CompletableFuture.completedFuture(AiInsightResult.success(content, aiConfig.getModelName()));
    }

    @Override
    @Async
    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackInsights")
    @Retry(name = "llm")
    public CompletableFuture<AiInsightResult> explainIsepResults(DashboardSnapshot snapshot) {
        log.info("[llm-groq] Explicando resultados ISEP para projeto={} questionário={}",
                snapshot.getProjectId(), snapshot.getQuestionnaireId());

        DashboardSnapshot sanitized = sanitizer.sanitize(snapshot);
        Prompt prompt = promptBuilder.buildExplainIsepPrompt(sanitized);

        ChatResponse response = chatModel.call(prompt);
        String content = response.getResult().getOutput().getText();

        log.info("[llm-groq] Explicação ISEP gerada para questionário={}",
                snapshot.getQuestionnaireId());
        return CompletableFuture.completedFuture(AiInsightResult.success(content, aiConfig.getModelName()));
    }

    @Override
    public void askQuestion(String question, DashboardSnapshot snapshot, SseEmitter emitter) {
        log.info("[llm-groq] Pergunta Q&A do usuário: '{}'", question);

        DashboardSnapshot sanitized = sanitizer.sanitize(snapshot);
        Prompt prompt = promptBuilder.buildQaPrompt(question, sanitized);

        try {
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
                        .data("Análise de IA temporariamente indisponível. Tente novamente."));
            } catch (Exception ignored) {}
            emitter.complete();
        }
    }

    @SuppressWarnings("unused")
    private CompletableFuture<AiInsightResult> fallbackInsights(
            DashboardSnapshot snapshot, Throwable t) {
        log.warn("[llm-groq] Fallback ativado para projeto={}: {}",
                snapshot.getProjectId(), t.getMessage());
        return CompletableFuture.completedFuture(
                AiInsightResult.unavailable(
                        "Análise de IA temporariamente indisponível. " +
                        "Os dados do dashboard continuam disponíveis normalmente. " +
                        "Tente novamente em alguns instantes.")
        );
    }
}

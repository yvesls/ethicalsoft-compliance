package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import com.ethicalsoft.ethicalsoft_complience.application.port.ai.LlmAnalysisPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class NoOpLlmAdapter implements LlmAnalysisPort {

    private static final String UNAVAILABLE_MSG =
            "Módulo de IA não configurado. Defina GROQ_API_KEY e AI_ENABLED=true para ativar.";

    @Override
    public CompletableFuture<AiInsightResult> generateInsights(DashboardSnapshot snapshot) {
        log.debug("[llm-noop] Insights solicitados — IA desabilitada");
        return CompletableFuture.completedFuture(AiInsightResult.unavailable(UNAVAILABLE_MSG));
    }

    @Override
    public CompletableFuture<AiInsightResult> generateRiskReport(DashboardSnapshot snapshot) {
        log.debug("[llm-noop] Relatório de risco solicitado — IA desabilitada");
        return CompletableFuture.completedFuture(AiInsightResult.unavailable(UNAVAILABLE_MSG));
    }

    @Override
    public CompletableFuture<AiInsightResult> explainIsepResults(DashboardSnapshot snapshot) {
        log.debug("[llm-noop] Explicação ISEP solicitada — IA desabilitada");
        return CompletableFuture.completedFuture(AiInsightResult.unavailable(UNAVAILABLE_MSG));
    }

    @Override
    public void askQuestion(String question, DashboardSnapshot snapshot, SseEmitter emitter) {
        log.debug("[llm-noop] Q&A solicitado — IA desabilitada");
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(UNAVAILABLE_MSG));
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}


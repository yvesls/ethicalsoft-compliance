package com.ethicalsoft.ethicalsoft_complience.application.port.ai;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface LlmAnalysisPort {

    CompletableFuture<AiInsightResult> generateInsights(DashboardSnapshot snapshot, String language, Long userId);

    CompletableFuture<AiInsightResult> generateRiskReport(DashboardSnapshot snapshot, String language, Long userId);

    CompletableFuture<AiInsightResult> explainIsepResults(DashboardSnapshot snapshot, String language, Long userId);

    void askQuestion(String question, DashboardSnapshot snapshot, SseEmitter emitter, String language, Long userId)
            throws IOException;

    default boolean isAvailable() {
        return false;
    }

    default boolean isAvailableForUser(Long userId) {
        return false;
    }

    default String translate(String text, String targetLanguage, Long userId) {
        return text;
    }
}

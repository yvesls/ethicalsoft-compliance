package com.ethicalsoft.ethicalsoft_complience.application.port.ai;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface LlmAnalysisPort {

    CompletableFuture<AiInsightResult> generateInsights(DashboardSnapshot snapshot, String language);

    CompletableFuture<AiInsightResult> generateRiskReport(DashboardSnapshot snapshot, String language);

    CompletableFuture<AiInsightResult> explainIsepResults(DashboardSnapshot snapshot, String language);

    void askQuestion(String question, DashboardSnapshot snapshot, SseEmitter emitter, String language)
            throws IOException;

    default boolean isAvailable() {
        return false;
    }

    default String translate(String text, String targetLanguage) {
        return text;
    }
}

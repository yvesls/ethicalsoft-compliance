package com.ethicalsoft.ethicalsoft_complience.application.port.ai;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

public interface LlmAnalysisPort {

    CompletableFuture<AiInsightResult> generateInsights(DashboardSnapshot snapshot);

    CompletableFuture<AiInsightResult> generateRiskReport(DashboardSnapshot snapshot);

    CompletableFuture<AiInsightResult> explainIsepResults(DashboardSnapshot snapshot);

    void askQuestion(String question, DashboardSnapshot snapshot, SseEmitter emitter);
}


package com.ethicalsoft.ethicalsoft_complience.application.usecase.ai;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import com.ethicalsoft.ethicalsoft_complience.application.port.ai.LlmAnalysisPort;
import com.ethicalsoft.ethicalsoft_complience.application.service.ai.AiDashboardContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateAiInsightsUseCase {

    private final AiDashboardContextProvider contextProvider;
    private final LlmAnalysisPort llmAnalysisPort;

    public CompletableFuture<AiInsightResult> execute(Long projectId, Integer questionnaireId,
                                                      String language, Long userId) {
        log.info("[ai-insights] Gerando insights IA projeto={} questionário={} idioma={} userId={}",
                projectId, questionnaireId, language, userId);

        DashboardSnapshot snapshot = contextProvider.buildSnapshot(projectId, questionnaireId);
        return llmAnalysisPort.generateInsights(snapshot, language, userId);
    }
}

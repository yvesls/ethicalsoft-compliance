package com.ethicalsoft.ethicalsoft_complience.application.usecase.ai;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import com.ethicalsoft.ethicalsoft_complience.application.port.ai.LlmAnalysisPort;
import com.ethicalsoft.ethicalsoft_complience.application.service.ai.AiDashboardContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AskDashboardQuestionUseCase {

    private final AiDashboardContextProvider contextProvider;
    private final LlmAnalysisPort llmAnalysisPort;

    public void execute(Long projectId, Integer questionnaireId, String question,
                        SseEmitter emitter, String language, Long userId) {
        log.info("[ai-qa] Pergunta Q&A projeto={} questionário={} idioma={} userId={}: '{}'",
                projectId, questionnaireId, language, userId, question);

        DashboardSnapshot snapshot = contextProvider.buildSnapshot(projectId, questionnaireId);

        CompletableFuture.runAsync(() ->
                llmAnalysisPort.askQuestion(question, snapshot, emitter, language, userId)
        );
    }
}

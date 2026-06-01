package com.ethicalsoft.ethicalsoft_complience.application.usecase.ai;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.QuestionContextAnalyzer;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.QuestionAnalysis;
import com.ethicalsoft.ethicalsoft_complience.application.port.ai.LlmAnalysisPort;
import com.ethicalsoft.ethicalsoft_complience.application.service.ai.AiDashboardContextProvider;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AskDashboardQuestionUseCase {

    private final AiDashboardContextProvider contextProvider;
    private final LlmAnalysisPort llmAnalysisPort;
    private final QuestionContextAnalyzer questionContextAnalyzer;

    @Value("${app.ai.timeout-seconds:60}")
    private int timeoutSeconds;

    public SseEmitter execute(@NonNull Long projectId, Integer questionnaireId, String question, String language) {
        log.info("[ai-qa] Pergunta Q&A projeto={} questionário={} idioma={}: '{}'",
                projectId, questionnaireId, language, question);

        QuestionAnalysis analysis = questionContextAnalyzer.analyze(question);
        DashboardSnapshot snapshot = contextProvider.buildContextualSnapshot(projectId, questionnaireId, analysis);

        SseEmitter emitter = new SseEmitter((long) timeoutSeconds * 1000);
        CompletableFuture.runAsync(() ->
                {
                    try {
                        llmAnalysisPort.askQuestion(question, snapshot, emitter, language);
                    } catch (IOException e) {
                        throw new BusinessException("Erro inesperado no processo de pergunta ao módulo de IA.");
                    }
                }
        );

        return emitter;
    }
}

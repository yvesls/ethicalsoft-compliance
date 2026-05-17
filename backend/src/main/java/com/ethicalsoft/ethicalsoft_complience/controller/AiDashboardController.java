package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.AskDashboardQuestionUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.ExplainIsepResultsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.GenerateAiInsightsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.GenerateRiskReportUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AiStatusResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AskQuestionRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.infra.config.AiConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/projects/{projectId}/ai")
@RequiredArgsConstructor
@Slf4j
public class AiDashboardController {

    private final AiConfig aiConfig;
    private final GenerateAiInsightsUseCase generateInsightsUseCase;
    private final GenerateRiskReportUseCase generateRiskReportUseCase;
    private final ExplainIsepResultsUseCase explainIsepResultsUseCase;
    private final AskDashboardQuestionUseCase askQuestionUseCase;

    @GetMapping("/insights")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> getInsights(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId) {

        if (!aiConfig.isEnabled()) {
            return unavailableResponse();
        }

        log.info("[ai-controller] Insights IA projeto={} questionário={}", projectId, questionnaireId);
        return generateInsightsUseCase.execute(projectId, questionnaireId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/risk-report")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> getRiskReport(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId) {

        if (!aiConfig.isEnabled()) {
            return unavailableResponse();
        }

        log.info("[ai-controller] Relatório de risco projeto={} questionário={}", projectId, questionnaireId);
        return generateRiskReportUseCase.execute(projectId, questionnaireId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/explain")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> explainIsepResults(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId) {

        if (!aiConfig.isEnabled()) {
            return unavailableResponse();
        }

        log.info("[ai-controller] Explicação ISEP projeto={} questionário={}", projectId, questionnaireId);
        return explainIsepResultsUseCase.execute(projectId, questionnaireId)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public SseEmitter askQuestion(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId,
            @Valid @RequestBody AskQuestionRequestDTO request) {

        SseEmitter emitter = new SseEmitter((long) aiConfig.getTimeoutSeconds() * 1000);

        if (!aiConfig.isEnabled()) {
            completeWithUnavailableMessage(emitter);
            return emitter;
        }

        log.info("[ai-controller] Q&A projeto={} questionário={}: '{}'",
                projectId, questionnaireId, request.question());
        askQuestionUseCase.execute(projectId, questionnaireId, request.question(), emitter);
        return emitter;
    }

    @GetMapping("/status")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public AiStatusResponseDTO getAiStatus(@PathVariable Long projectId) {
        return new AiStatusResponseDTO(aiConfig.isEnabled(), "groq-cloud", aiConfig.getModelName());
    }

    private CompletableFuture<ResponseEntity<AiInsightResult>> unavailableResponse() {
        return CompletableFuture.completedFuture(
                ResponseEntity.ok(AiInsightResult.unavailable("Módulo de IA desabilitado.")));
    }

    private void completeWithUnavailableMessage(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("Módulo de IA desabilitado."));
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}

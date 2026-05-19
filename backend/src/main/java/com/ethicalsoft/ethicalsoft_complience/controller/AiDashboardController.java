package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.AskDashboardQuestionUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.CheckAiAvailabilityUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.ExplainIsepResultsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.GenerateAiInsightsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.GenerateRiskReportUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AiStatusResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AskQuestionRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/projects/{projectId}/ai")
@RequiredArgsConstructor
public class AiDashboardController {

    private final CheckAiAvailabilityUseCase checkAiAvailabilityUseCase;
    private final GenerateAiInsightsUseCase generateInsightsUseCase;
    private final GenerateRiskReportUseCase generateRiskReportUseCase;
    private final ExplainIsepResultsUseCase explainIsepResultsUseCase;
    private final AskDashboardQuestionUseCase askQuestionUseCase;

    @GetMapping("/insights")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> getInsights(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId) {
        return generateInsightsUseCase.execute(projectId, questionnaireId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/risk-report")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> getRiskReport(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId) {
        return generateRiskReportUseCase.execute(projectId, questionnaireId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/explain")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> explainIsepResults(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId) {
        return explainIsepResultsUseCase.execute(projectId, questionnaireId)
                .thenApply(ResponseEntity::ok);
    }

    /* TODO: Avaliar estado para dar continuidade posterirmente
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public SseEmitter askQuestion(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId,
            @Valid @RequestBody AskQuestionRequestDTO request) {
        return askQuestionUseCase.execute(projectId, questionnaireId, request.question());
    }
    */

    @GetMapping("/status")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<AiStatusResponseDTO> getAiStatus(@PathVariable Long projectId) {
        return ResponseEntity.ok(checkAiAvailabilityUseCase.execute());
    }
}

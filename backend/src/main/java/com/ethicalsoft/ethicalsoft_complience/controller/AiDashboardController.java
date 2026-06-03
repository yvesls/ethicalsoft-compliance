package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.AskDashboardQuestionUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.CheckAiAvailabilityUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.ExplainIsepResultsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.GenerateAiInsightsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.GenerateRiskReportUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AiStatusResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.i18n.SupportedLanguage;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam Integer questionnaireId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @AuthenticationPrincipal User currentUser) {
        Long userId = requireUserId(currentUser);
        return generateInsightsUseCase.execute(projectId, questionnaireId, resolveLanguage(acceptLanguage), userId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/risk-report")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> getRiskReport(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @AuthenticationPrincipal User currentUser) {
        Long userId = requireUserId(currentUser);
        return generateRiskReportUseCase.execute(projectId, questionnaireId, resolveLanguage(acceptLanguage), userId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/explain")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> explainIsepResults(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @AuthenticationPrincipal User currentUser) {
        Long userId = requireUserId(currentUser);
        return explainIsepResultsUseCase.execute(projectId, questionnaireId, resolveLanguage(acceptLanguage), userId)
                .thenApply(ResponseEntity::ok);
    }

    /* TODO: Avaliar estado para dar continuidade posterirmente
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public SseEmitter askQuestion(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @Valid @RequestBody AskQuestionRequestDTO request,
            @AuthenticationPrincipal User currentUser) {
        return askQuestionUseCase.execute(projectId, questionnaireId, request.question(),
                resolveLanguage(acceptLanguage), requireUserId(currentUser));
    }
    */

    private String resolveLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return SupportedLanguage.PT_BR.code();
        }
        String first = acceptLanguage.split(",")[0].trim();
        return first.isBlank() ? SupportedLanguage.PT_BR.code() : first;
    }

    private Long requireUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("ai.token.unauthenticated",
                    "É necessário estar autenticado para utilizar os recursos de IA.");
        }
        return user.getId();
    }

    @GetMapping("/status")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<AiStatusResponseDTO> getAiStatus(@PathVariable Long projectId,
                                                           @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(checkAiAvailabilityUseCase.execute(currentUser != null ? currentUser.getId() : null));
    }
}

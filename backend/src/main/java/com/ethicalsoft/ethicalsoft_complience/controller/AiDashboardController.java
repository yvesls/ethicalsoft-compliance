package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.AiInsightResult;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.AskDashboardQuestionUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.ExplainIsepResultsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.GenerateAiInsightsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ai.GenerateRiskReportUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AiStatusResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.UserAiTokenRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.ai.AskQuestionRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.i18n.SupportedLanguage;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.infra.config.AiConfig;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final UserAiTokenRepository userAiTokenRepository;

    @GetMapping("/insights")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> getInsights(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @AuthenticationPrincipal User currentUser) {

        if (!aiConfig.isEnabled()) {
            return unavailableResponse();
        }

        Long userId = requireUserId(currentUser);
        String language = resolveLanguage(acceptLanguage);
        log.info("[ai-controller] Insights IA projeto={} questionário={} idioma={} userId={}",
                projectId, questionnaireId, language, userId);
        return generateInsightsUseCase.execute(projectId, questionnaireId, language, userId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/risk-report")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> getRiskReport(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @AuthenticationPrincipal User currentUser) {

        if (!aiConfig.isEnabled()) {
            return unavailableResponse();
        }

        Long userId = requireUserId(currentUser);
        String language = resolveLanguage(acceptLanguage);
        log.info("[ai-controller] Relatório de risco projeto={} questionário={} idioma={} userId={}",
                projectId, questionnaireId, language, userId);
        return generateRiskReportUseCase.execute(projectId, questionnaireId, language, userId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/explain")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public CompletableFuture<ResponseEntity<AiInsightResult>> explainIsepResults(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @AuthenticationPrincipal User currentUser) {

        if (!aiConfig.isEnabled()) {
            return unavailableResponse();
        }

        Long userId = requireUserId(currentUser);
        String language = resolveLanguage(acceptLanguage);
        log.info("[ai-controller] Explicação ISEP projeto={} questionário={} idioma={} userId={}",
                projectId, questionnaireId, language, userId);
        return explainIsepResultsUseCase.execute(projectId, questionnaireId, language, userId)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public SseEmitter askQuestion(
            @PathVariable Long projectId,
            @RequestParam Integer questionnaireId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @Valid @RequestBody AskQuestionRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        SseEmitter emitter = new SseEmitter((long) aiConfig.getTimeoutSeconds() * 1000);

        if (!aiConfig.isEnabled()) {
            completeWithUnavailableMessage(emitter);
            return emitter;
        }

        Long userId = requireUserId(currentUser);
        String language = resolveLanguage(acceptLanguage);
        log.info("[ai-controller] Q&A projeto={} questionário={} idioma={} userId={}: '{}'",
                projectId, questionnaireId, language, userId, request.question());
        askQuestionUseCase.execute(projectId, questionnaireId, request.question(), emitter, language, userId);
        return emitter;
    }

    private String resolveLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return SupportedLanguage.PT_BR.code();
        }
        String first = acceptLanguage.split(",")[0].trim();
        return first.isBlank() ? SupportedLanguage.PT_BR.code() : first;
    }

    private Long requireUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(
                    "É necessário estar autenticado para utilizar os recursos de IA.");
        }
        return user.getId();
    }

    @GetMapping("/status")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public AiStatusResponseDTO getAiStatus(@PathVariable Long projectId,
                                           @AuthenticationPrincipal User currentUser) {
        boolean tokenConfigured = currentUser != null && currentUser.getId() != null
                && safeHasToken(currentUser.getId());
        return new AiStatusResponseDTO(aiConfig.isEnabled(), "groq-cloud",
                aiConfig.getModelName(), tokenConfigured);
    }

    private boolean safeHasToken(Long userId) {
        try {
            return userAiTokenRepository.existsByUserId(userId);
        } catch (Exception ex) {
            return false;
        }
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

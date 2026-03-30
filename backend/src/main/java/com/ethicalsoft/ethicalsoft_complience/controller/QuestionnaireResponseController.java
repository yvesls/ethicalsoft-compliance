package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireAnswersRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireAnswersResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireResponseSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.GetQuestionnaireAnswersPageUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ListQuestionnaireQuestionsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ListQuestionnaireSummariesUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.SubmitQuestionnaireAnswersPageUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.GetConsolidatedAnswersUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.GetRepresentativeResponsesUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.ConsolidatedAnswerDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.RepresentativeResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/projects/{projectId}/questionnaires/{questionnaireId}")
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireResponseController {

    private final ListQuestionnaireQuestionsUseCase listQuestionnaireQuestionsUseCase;
    private final GetQuestionnaireAnswersPageUseCase getQuestionnaireAnswersPageUseCase;
    private final SubmitQuestionnaireAnswersPageUseCase submitQuestionnaireAnswersPageUseCase;
    private final ListQuestionnaireSummariesUseCase listQuestionnaireSummariesUseCase;
    private final GetRepresentativeResponsesUseCase getRepresentativeResponsesUseCase;
    private final GetConsolidatedAnswersUseCase getConsolidatedAnswersUseCase;

    @GetMapping("/questions")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public Page<QuestionnaireQuestionResponseDTO> listQuestions(@PathVariable Long projectId,
                                                                @PathVariable Integer questionnaireId,
                                                                @PageableDefault(size = 10) Pageable pageable,
                                                                @RequestParam(required = false) String questionText,
                                                                @RequestParam(required = false) String roleName,
                                                                @RequestParam(required = false) Long representativeId) {
        return listQuestionnaireQuestionsUseCase.execute(projectId, questionnaireId, pageable, questionText, roleName, representativeId);
    }

    @GetMapping("/responses")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public QuestionnaireAnswersResponseDTO getAnswers(@PathVariable Long projectId,
                                                      @PathVariable Integer questionnaireId) {
        return getQuestionnaireAnswersPageUseCase.execute(projectId, questionnaireId);
    }

    @PostMapping("/responses")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public QuestionnaireAnswersResponseDTO submitAnswers(@PathVariable Long projectId,
                                                         @PathVariable Integer questionnaireId,
                                                         @Valid @RequestBody QuestionnaireAnswersRequestDTO request) {
        return submitQuestionnaireAnswersPageUseCase.execute(projectId, questionnaireId, request);
    }

    @GetMapping("/responses/summaries")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public List<QuestionnaireResponseSummaryDTO> listSummaries(@PathVariable Long projectId,
                                                               @PathVariable Integer questionnaireId) {
        return listQuestionnaireSummariesUseCase.execute(projectId, questionnaireId);
    }

    @GetMapping("/responses/representative/{representativeId}")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public RepresentativeResponseDTO getRepresentativeResponses(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId,
            @PathVariable Long representativeId) {
        log.info("[controller] Busca respostas representante={} questionário={} projeto={}",
                representativeId, questionnaireId, projectId);
        return getRepresentativeResponsesUseCase.execute(projectId, questionnaireId, representativeId);
    }

    @GetMapping("/responses/consolidated")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public Page<ConsolidatedAnswerDTO> getConsolidatedAnswers(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId,
            @RequestParam(required = false) Long representativeId,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Boolean response,
            @RequestParam(required = false) String questionText,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("[controller] Respostas consolidadas questionário={} projeto={}", questionnaireId, projectId);
        return getConsolidatedAnswersUseCase.executeForQuestionnaire(
                projectId, questionnaireId, representativeId, questionId, roleId, roleName, response, questionText, pageable);
    }
}

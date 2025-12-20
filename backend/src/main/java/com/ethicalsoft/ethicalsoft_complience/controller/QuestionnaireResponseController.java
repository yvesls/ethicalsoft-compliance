package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.GetQuestionnaireAnswersPageUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ListQuestionnaireQuestionsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ListQuestionnaireSummariesUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.SubmitQuestionnaireAnswersPageUseCase;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireAnswerPageRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireAnswerPageResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireResponseSummaryDTO;
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

    @GetMapping("/questions")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public Page<QuestionnaireQuestionResponseDTO> listQuestions(@PathVariable Long projectId,
                                                                @PathVariable Integer questionnaireId,
                                                                @PageableDefault(size = 10) Pageable pageable,
                                                                @RequestParam(required = false) String questionText,
                                                                @RequestParam(required = false) String roleName) {
        return listQuestionnaireQuestionsUseCase.execute(questionnaireId, pageable, questionText, roleName);
    }

    @GetMapping("/responses/page")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public QuestionnaireAnswerPageResponseDTO getAnswersPage(@PathVariable Long projectId,
                                                             @PathVariable Integer questionnaireId,
                                                             @PageableDefault(size = 10) Pageable pageable) {
        return getQuestionnaireAnswersPageUseCase.execute(projectId, questionnaireId, pageable);
    }

    @PostMapping("/responses/page")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public QuestionnaireAnswerPageResponseDTO submitAnswerPage(@PathVariable Long projectId,
                                                               @PathVariable Integer questionnaireId,
                                                               @Valid @RequestBody QuestionnaireAnswerPageRequestDTO request) {
        return submitQuestionnaireAnswersPageUseCase.execute(projectId, questionnaireId, request);
    }

    @GetMapping("/responses/summaries")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public List<QuestionnaireResponseSummaryDTO> listSummaries(@PathVariable Long projectId,
                                                               @PathVariable Integer questionnaireId) {
        return listQuestionnaireSummariesUseCase.execute(projectId, questionnaireId);
    }
}

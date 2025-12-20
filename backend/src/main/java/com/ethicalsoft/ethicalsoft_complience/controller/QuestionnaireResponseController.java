package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireAnswerPageRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireAnswerPageResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireResponseSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.service.QuestionnaireResponseService;
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

    private final QuestionnaireResponseService questionnaireResponseService;

    @GetMapping("/questions")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public Page<QuestionnaireQuestionResponseDTO> listQuestions(@PathVariable Long projectId,
                                                                @PathVariable Integer questionnaireId,
                                                                @PageableDefault(size = 10) Pageable pageable) {
        return questionnaireResponseService.listQuestions(projectId, questionnaireId, pageable);
    }

    @GetMapping("/responses/page")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public QuestionnaireAnswerPageResponseDTO getAnswersPage(@PathVariable Long projectId,
                                                             @PathVariable Integer questionnaireId,
                                                             @PageableDefault(size = 10) Pageable pageable) {
        return questionnaireResponseService.getAnswerPage(projectId, questionnaireId, pageable);
    }

    @PostMapping("/responses/page")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public QuestionnaireAnswerPageResponseDTO submitAnswerPage(@PathVariable Long projectId,
                                                               @PathVariable Integer questionnaireId,
                                                               @Valid @RequestBody QuestionnaireAnswerPageRequestDTO request) {
        return questionnaireResponseService.submitAnswerPage(projectId, questionnaireId, request);
    }

    @GetMapping("/responses/summaries")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public List<QuestionnaireResponseSummaryDTO> listSummaries(@PathVariable Long projectId,
                                                              @PathVariable Integer questionnaireId) {
        return questionnaireResponseService.listSummaries(projectId, questionnaireId);
    }
}

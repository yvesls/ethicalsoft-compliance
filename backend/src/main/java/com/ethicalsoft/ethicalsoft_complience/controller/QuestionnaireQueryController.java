package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireRawResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.service.QuestionnaireQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/questionnaires")
@RequiredArgsConstructor
public class QuestionnaireQueryController {

    private final QuestionnaireQueryService questionnaireQueryService;

    @GetMapping("/{questionnaireId}/raw")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public QuestionnaireRawResponseDTO getQuestionnaireRaw(@PathVariable Long projectId,
                                                           @PathVariable Integer questionnaireId) {
        return questionnaireQueryService.getQuestionnaireRaw(projectId, questionnaireId);
    }

    @PostMapping("/{questionnaireId}/questions/search")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public Page<QuestionnaireQuestionResponseDTO> searchQuestions(@PathVariable Long projectId,
                                                                  @PathVariable Integer questionnaireId,
                                                                  @RequestBody(required = false) QuestionSearchFilterDTO filter,
                                                                  @PageableDefault(size = 10) Pageable pageable) {
        return questionnaireQueryService.searchQuestions(projectId, questionnaireId, filter, pageable);
    }
}

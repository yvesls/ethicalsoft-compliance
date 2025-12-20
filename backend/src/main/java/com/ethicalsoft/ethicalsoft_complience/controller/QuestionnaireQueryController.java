package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.GetQuestionnaireRawUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.SearchQuestionnaireQuestionsUseCase;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionSearchFilterDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireRawResponseDTO;
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

    private final GetQuestionnaireRawUseCase getQuestionnaireRawUseCase;
    private final SearchQuestionnaireQuestionsUseCase searchQuestionnaireQuestionsUseCase;

    @GetMapping("/{questionnaireId}/raw")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public QuestionnaireRawResponseDTO getQuestionnaireRaw(@PathVariable Long projectId,
                                                           @PathVariable Integer questionnaireId) {
        return getQuestionnaireRawUseCase.execute(projectId, questionnaireId);
    }

    @PostMapping("/{questionnaireId}/questions/search")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public Page<QuestionnaireQuestionResponseDTO> searchQuestions(@PathVariable Long projectId,
                                                                  @PathVariable Integer questionnaireId,
                                                                  @RequestBody(required = false) QuestionSearchFilterDTO filter,
                                                                  @PageableDefault(size = 10) Pageable pageable) {
        return searchQuestionnaireQuestionsUseCase.execute(projectId, questionnaireId, filter, pageable);
    }
}

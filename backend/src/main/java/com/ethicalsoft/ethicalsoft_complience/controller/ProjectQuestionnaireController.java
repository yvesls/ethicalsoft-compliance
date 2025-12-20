package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.GetProjectQuestionnaireSummaryUseCase;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectQuestionnaireSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/projects/{projectId}/questionnaires/{questionnaireId}")
@RequiredArgsConstructor
public class ProjectQuestionnaireController {

    private final GetProjectQuestionnaireSummaryUseCase getProjectQuestionnaireSummaryUseCase;

    @GetMapping
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ProjectQuestionnaireSummaryDTO getQuestionnaireSummary(@PathVariable Long projectId,
                                                                  @PathVariable Integer questionnaireId) {
        return getProjectQuestionnaireSummaryUseCase.execute(projectId, questionnaireId);
    }
}

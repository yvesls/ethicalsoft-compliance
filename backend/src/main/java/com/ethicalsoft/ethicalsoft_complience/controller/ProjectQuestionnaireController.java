package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectQuestionnaireSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ListProjectQuestionnairesUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.project.GetProjectQuestionnaireSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/projects/{projectId}/questionnaires")
@RequiredArgsConstructor
public class ProjectQuestionnaireController {

    private final GetProjectQuestionnaireSummaryUseCase getProjectQuestionnaireSummaryUseCase;
    private final ListProjectQuestionnairesUseCase listProjectQuestionnairesUseCase;

    @GetMapping("/{questionnaireId}")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ProjectQuestionnaireSummaryDTO getQuestionnaireSummary(@PathVariable Long projectId,
                                                                  @PathVariable Integer questionnaireId) {
        return getProjectQuestionnaireSummaryUseCase.execute(projectId, questionnaireId);
    }

    @GetMapping
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public Page<QuestionnaireSummaryResponseDTO> listQuestionnaires(@PathVariable Long projectId,
                                                                    @PageableDefault(size = 10) Pageable pageable,
                                                                    @RequestParam(required = false) String name,
                                                                    @RequestParam(required = false) String stageName,
                                                                    @RequestParam(required = false) String iterationName) {
        QuestionnaireSearchFilter filter = new QuestionnaireSearchFilter(name, stageName, iterationName);
        return listProjectQuestionnairesUseCase.execute(projectId, pageable, filter);
    }
}

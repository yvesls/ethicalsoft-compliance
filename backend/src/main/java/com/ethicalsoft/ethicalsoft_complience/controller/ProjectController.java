package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireReminderRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping( "api/projects" )
@RequiredArgsConstructor
public class ProjectController {

    private final ListRolesUseCase listRolesUseCase;
    private final CreateProjectUseCase createProjectUseCase;
    private final SearchProjectsUseCase searchProjectsUseCase;
    private final GetProjectByIdUseCase getProjectByIdUseCase;
    private final ListProjectQuestionnairesUseCase listProjectQuestionnairesUseCase;
    private final SendQuestionnaireReminderUseCase sendQuestionnaireReminderUseCase;

    @GetMapping("/roles")
    public List<RoleSummaryResponseDTO> listRoles() {
        return listRolesUseCase.execute();
    }

    @PostMapping
    public ProjectResponseDTO createProject( @Valid @RequestBody ProjectCreationRequestDTO request ) {
        return createProjectUseCase.execute( request );
    }

    @PostMapping("/search")
    public Page<ProjectSummaryResponseDTO> getAllProjects(
            @RequestBody ProjectSearchRequestDTO filters,
            @PageableDefault(page = 0, size = 10, sort = "name") Pageable pageable) {
        return searchProjectsUseCase.execute(filters, pageable);
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ProjectDetailResponseDTO getProjectById(@PathVariable Long projectId) {
        return getProjectByIdUseCase.execute(projectId);
    }

    @GetMapping("/{projectId}/questionnaires")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public Page<QuestionnaireSummaryResponseDTO> listProjectQuestionnaires(
            @PathVariable Long projectId,
            @RequestParam(name = "name", required = false) String questionnaireName,
            @RequestParam(name = "stage", required = false) String stageName,
            @RequestParam(name = "iteration", required = false) String iterationName,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        QuestionnaireSearchFilter filter = new QuestionnaireSearchFilter(questionnaireName, stageName, iterationName);
        return listProjectQuestionnairesUseCase.execute(projectId, pageable, filter);
    }

    @PostMapping("/{projectId}/questionnaires/{questionnaireId}/reminders")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public void sendQuestionnaireReminder(@PathVariable Long projectId,
                                          @PathVariable Integer questionnaireId,
                                          @Valid @RequestBody QuestionnaireReminderRequestDTO requestDTO) {
        sendQuestionnaireReminderUseCase.execute(projectId, questionnaireId, requestDTO);
    }
}
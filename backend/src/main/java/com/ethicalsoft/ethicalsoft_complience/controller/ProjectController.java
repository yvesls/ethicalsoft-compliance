package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireReminderRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.UpdateProjectRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.*;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ListProjectQuestionnairesUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.ListRolesUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.project.*;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ListRolesUseCase listRolesUseCase;
    private final CreateProjectUseCase createProjectUseCase;
    private final SearchProjectsUseCase searchProjectsUseCase;
    private final GetProjectByIdUseCase getProjectByIdUseCase;
    private final ListProjectQuestionnairesUseCase listProjectQuestionnairesUseCase;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final CloseProjectManuallyUseCase closeProjectManuallyUseCase;
    private final PublishDraftProjectUseCase publishDraftProjectUseCase;
    private final UpdateDraftProjectUseCase updateDraftProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final GetProjectEditSnapshotUseCase getProjectEditSnapshotUseCase;

    @GetMapping("/roles")
    public List<RoleSummaryResponseDTO> listRoles() {
        return listRolesUseCase.execute();
    }

    @PostMapping
    public ProjectResponseDTO createProject(@Valid @RequestBody ProjectCreationRequestDTO request ) {
        return createProjectUseCase.createProject( request );
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

    @GetMapping("/{projectId}/edit")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ProjectEditSnapshotDTO getProjectEditSnapshot(@PathVariable Long projectId) {
        return getProjectEditSnapshotUseCase.execute(projectId);
    }

    @PostMapping("/{projectId}/questionnaires/{questionnaireId}/reminders")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public void sendQuestionnaireReminder(@PathVariable Long projectId,
                                          @PathVariable Integer questionnaireId,
                                          @Valid @RequestBody QuestionnaireReminderRequestDTO requestDTO) {
        sendNotificationUseCase.execute(new com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand(
                NotificationType.QUESTIONNAIRE_REMINDER,
                java.util.Map.of(
                        "projectId", projectId,
                        "questionnaireId", questionnaireId,
                        "recipients", requestDTO.emails()
                )
        ));
    }

    @PostMapping("/{projectId}/publish")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ProjectResponseDTO publishDraftProject(@PathVariable Long projectId) {
        return publishDraftProjectUseCase.execute(projectId);
    }

    @PutMapping("/{projectId}/draft")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ProjectResponseDTO updateDraftProject(@PathVariable Long projectId,
                                                  @Valid @RequestBody ProjectCreationRequestDTO request) {
        return updateDraftProjectUseCase.execute(projectId, request);
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public UpdateProjectResponseDTO updateProject(@PathVariable Long projectId,
                                                   @Valid @RequestBody UpdateProjectRequestDTO request) {
        return updateProjectUseCase.execute(projectId, request);
    }

    @PostMapping("/{projectId}/close")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<Map<String, Object>> closeProject(@PathVariable Long projectId) {
        var result = closeProjectManuallyUseCase.execute(projectId);
        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "isepPercent", IsepMath.toPercent(result.getIsep()).toPlainString(),
                "band", result.getBand(),
                "questionnaireCount", result.getQuestionnaireCount(),
                "calculatedAt", result.getCalculatedAt().toString(),
                "closedBy", result.getClosedBy() != null ? result.getClosedBy() : ""
        ));
    }
}
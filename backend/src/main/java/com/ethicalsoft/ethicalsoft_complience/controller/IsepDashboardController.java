package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.*;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/projects/{projectId}")
@RequiredArgsConstructor
public class IsepDashboardController {

    private final GetProjectIsepDashboardUseCase projectDashboardUseCase;
    private final GetQuestionnaireDashboardUseCase questionnaireDashboardUseCase;
    private final GetIndividualDashboardUseCase individualDashboardUseCase;
    private final GetRoleStageComplianceUseCase roleStageComplianceUseCase;
    private final GetJustificationWordCloudUseCase wordCloudUseCase;
    private final ExportIsepDataUseCase exportIsepDataUseCase;
    private final ExportIsepCsvUseCase exportIsepCsvUseCase;
    private final GetConsolidatedAnswersUseCase consolidatedAnswersUseCase;

    @GetMapping("/dashboard")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ProjectIsepDashboardDTO getProjectDashboard(@PathVariable Long projectId) {
        return projectDashboardUseCase.execute(projectId);
    }

    @GetMapping("/questionnaires/{questionnaireId}/dashboard")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public QuestionnaireIsepDashboardDTO getQuestionnaireDashboard(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId) {
        return questionnaireDashboardUseCase.execute(projectId, questionnaireId);
    }

    @GetMapping("/questionnaires/{questionnaireId}/dashboard/role-stage")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public List<RoleStageComplianceDTO> getRoleStageCompliance(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId) {
        return roleStageComplianceUseCase.execute(projectId, questionnaireId);
    }

    @GetMapping("/questionnaires/{questionnaireId}/dashboard/word-cloud")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public WordCloudDTO getWordCloud(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId) {
        return wordCloudUseCase.execute(projectId, questionnaireId);
    }

    @GetMapping("/questionnaires/{questionnaireId}/dashboard/individual")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public IndividualDashboardDTO getIndividualDashboard(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId,
            @RequestParam Long representativeId) {
        return individualDashboardUseCase.execute(projectId, questionnaireId, representativeId);
    }

    @GetMapping("/dashboard/export")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public List<IsepDataExportDTO> exportProjectData(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "true") boolean anonymize) {
        return exportIsepDataUseCase.executeAll(projectId, anonymize);
    }

    @GetMapping("/questionnaires/{questionnaireId}/dashboard/export")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public IsepDataExportDTO exportQuestionnaireData(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId,
            @RequestParam(defaultValue = "true") boolean anonymize) {
        return exportIsepDataUseCase.execute(projectId, questionnaireId, anonymize);
    }

    @GetMapping(value = "/dashboard/export/csv", produces = "text/csv")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<String> exportProjectCsv(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "true") boolean anonymize) {
        String csv = exportIsepCsvUseCase.executeAll(projectId, anonymize);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"isep-projeto-" + projectId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping(value = "/questionnaires/{questionnaireId}/dashboard/export/csv", produces = "text/csv")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<String> exportQuestionnaireCsv(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId,
            @RequestParam(defaultValue = "true") boolean anonymize) {
        String csv = exportIsepCsvUseCase.execute(projectId, questionnaireId, anonymize);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"isep-questionario-" + questionnaireId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/responses/consolidated")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public Page<ConsolidatedAnswerDTO> getProjectConsolidatedAnswers(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer questionnaireId,
            @RequestParam(required = false) Long representativeId,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Boolean response,
            @RequestParam(required = false) String questionText,
            @PageableDefault(size = 20) Pageable pageable) {
        return consolidatedAnswersUseCase.executeForProject(
                projectId, questionnaireId, representativeId, questionId, roleId, roleName, response, questionText, pageable);
    }
}

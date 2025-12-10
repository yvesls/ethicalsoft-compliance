package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.infra.security.ProjectRoleAuthorizationEvaluator;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.*;
import com.ethicalsoft.ethicalsoft_complience.service.ProjectService;
import com.ethicalsoft.ethicalsoft_complience.service.facade.ProjectFacade;
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

	private final ProjectFacade projectFacade;
	private final ProjectService projectService;
	private final ProjectRoleAuthorizationEvaluator projectRoleAuthorizationEvaluator;

	@GetMapping("/roles")
	public List<RoleSummaryResponseDTO> listRoles() {
		return projectService.listRoleSummaries();
	}

	@PostMapping
	public ProjectResponseDTO createProject(@RequestBody ProjectCreationRequestDTO request ) {
		return projectFacade.createProject( request );
	}

	@PostMapping("/search")
	public Page<ProjectSummaryResponseDTO> getAllProjects(
			@RequestBody ProjectSearchRequestDTO filters,
			@PageableDefault(page = 0, size = 10, sort = "name") Pageable pageable) {
		return projectService.getAllProjectSummaries(filters, pageable);
	}

	@GetMapping("/{projectId}")
	@PreAuthorize("@projectRoleAuthorizationEvaluator.canAccess(authentication)")
	public ProjectDetailResponseDTO getProjectById(@PathVariable Long projectId) {
		return projectService.getProjectById(projectId);
	}

	@GetMapping("/{projectId}/questionnaires")
	@PreAuthorize("@projectRoleAuthorizationEvaluator.canAccess(authentication)")
	public Page<QuestionnaireSummaryResponseDTO> listProjectQuestionnaires(
			@PathVariable Long projectId,
			@PageableDefault(page = 0, size = 10) Pageable pageable) {
		return projectService.listQuestionnaires(projectId, pageable);
	}
}
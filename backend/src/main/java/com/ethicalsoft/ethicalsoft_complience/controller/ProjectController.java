package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectResponse;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.service.ProjectService;
import com.ethicalsoft.ethicalsoft_complience.service.facade.ProjectFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "projects" )
@RequiredArgsConstructor
public class ProjectController {

	private final ProjectFacade projectFacade;
	private final ProjectService projectService;

	@PostMapping
	public ProjectResponse createProject( @RequestBody ProjectCreationRequestDTO request ) {
		return projectFacade.createProject( request );
	}

	@PostMapping("/search")
	public Page<ProjectSummaryResponseDTO> getAllProjects(
			@RequestBody ProjectSearchRequestDTO filters,
			@PageableDefault(page = 0, size = 10, sort = "name") Pageable pageable) {
		return projectService.getAllProjectSummaries(filters, pageable);
	}
}
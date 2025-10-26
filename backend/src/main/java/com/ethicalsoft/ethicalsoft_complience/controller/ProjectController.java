package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequest;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectResponse;
import com.ethicalsoft.ethicalsoft_complience.service.facade.ProjectFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "projects" )
@RequiredArgsConstructor
public class ProjectController {

	private final ProjectFacade projectFacade;

	@PostMapping
	public ProjectResponse createProject( @RequestBody ProjectCreationRequest request ) {
		return projectFacade.createProject( request );
	}
}
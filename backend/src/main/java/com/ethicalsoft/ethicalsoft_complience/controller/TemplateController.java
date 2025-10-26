package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateListDTO;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateStageDTO;
import com.ethicalsoft.ethicalsoft_complience.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping( "templates" )
@RequiredArgsConstructor
public class TemplateController {

	private final TemplateService templateService;

	@GetMapping
	public List<TemplateListDTO> getAllTemplates() {
		return templateService.findAllTemplates();
	}

	@GetMapping( "/{project-id}/stages" )
	public List<TemplateStageDTO> getTemplateStages( @PathVariable( "project-id" ) String projectId ) {
		return templateService.findTemplateStages( projectId );
	}

	@PostMapping( "/from-project/{project-id}/{template-name}" )
	public ProjectTemplate createTemplateFromProject( @PathVariable( "project-id" ) Long projectId, @PathVariable( "template-name" ) String templateName ) {
		return templateService.createTemplateFromProject( projectId, templateName );
	}
}
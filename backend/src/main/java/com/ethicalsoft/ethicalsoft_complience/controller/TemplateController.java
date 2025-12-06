package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateListDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.CreateTemplateRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

	private final TemplateService templateService;

	@GetMapping("")
	public List<TemplateListDTO> getAllTemplates() {
		return templateService.findAllTemplates();
	}

	@GetMapping("/{id}/full")
	public ProjectTemplate getFullTemplate(@PathVariable("id") String id) {
		return templateService.findFullTemplateById(id);
	}

	@PostMapping("/from-project/{project-id}")
	public ProjectTemplate createTemplateFromProject(
			@PathVariable("project-id") Long projectId,
			@RequestBody CreateTemplateRequestDTO request) {
		return templateService.createTemplateFromProject(projectId, request);
	}
}
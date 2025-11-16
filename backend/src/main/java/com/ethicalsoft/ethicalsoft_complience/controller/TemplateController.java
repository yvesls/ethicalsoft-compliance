package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.*;
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

	@GetMapping("/{id}/header")
	public ProjectTemplate getTemplateHeader(@PathVariable("id") String id) {
		return templateService.getTemplateHeader(id);
	}

	@GetMapping("/{id}/stages")
	public List<TemplateStageDTO> getTemplateStages(@PathVariable("id") String id) {
		return templateService.findTemplateStages(id);
	}

	@GetMapping("/{id}/iterations")
	public List<TemplateIterationDTO> getTemplateIterations( @PathVariable("id") String id) {
		return templateService.findTemplateIterations(id);
	}

	@GetMapping("/{id}/questionnaires")
	public List<TemplateQuestionnaireDTO> getTemplateQuestionnaires( @PathVariable("id") String id) {
		return templateService.findTemplateQuestionnaires(id);
	}

	@GetMapping("/{id}/representatives")
	public List<TemplateRepresentativeDTO> getTemplateRepresentatives( @PathVariable("id") String id) {
		return templateService.findTemplateRepresentatives(id);
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
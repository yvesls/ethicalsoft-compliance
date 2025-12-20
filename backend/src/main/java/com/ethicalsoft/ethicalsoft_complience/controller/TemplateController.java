package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.template.CreateTemplateFromProjectUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.template.GetFullTemplateUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.template.ListTemplatesUseCase;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateListDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.CreateTemplateRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final ListTemplatesUseCase listTemplatesUseCase;
    private final GetFullTemplateUseCase getFullTemplateUseCase;
    private final CreateTemplateFromProjectUseCase createTemplateFromProjectUseCase;

    @GetMapping("")
    public List<TemplateListDTO> getAllTemplates() {
        return listTemplatesUseCase.execute();
    }

    @GetMapping("/{id}/full")
    public ProjectTemplate getFullTemplate(@PathVariable("id") String id) {
        return getFullTemplateUseCase.execute(id);
    }

    @PostMapping("/from-project/{project-id}")
    public ProjectTemplate createTemplateFromProject(
            @PathVariable("project-id") Long projectId,
            @RequestBody CreateTemplateRequestDTO request) {
        return createTemplateFromProjectUseCase.execute(projectId, request);
    }
}
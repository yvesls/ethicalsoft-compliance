package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateListDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.CreateTemplateRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.template.TemplateCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.template.TemplateQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateQueryPort templateQueryPort;
    private final TemplateCommandPort templateCommandPort;

    @GetMapping("")
    public List<TemplateListDTO> getAllTemplates() {
        return templateQueryPort.findAllTemplates();
    }

    @GetMapping("/{id}/full")
    public ProjectTemplate getFullTemplate(@PathVariable("id") String id) {
        return templateQueryPort.findFullTemplateById(id);
    }

    @PostMapping("/from-project/{project-id}")
    public ProjectTemplate createTemplateFromProject(
            @PathVariable("project-id") Long projectId,
            @RequestBody CreateTemplateRequestDTO request) {
        return templateCommandPort.createTemplateFromProject(projectId, request);
    }
}
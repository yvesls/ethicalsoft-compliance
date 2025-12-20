package com.ethicalsoft.ethicalsoft_complience.application.usecase.template;

import com.ethicalsoft.ethicalsoft_complience.application.port.TemplateCommandPort;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.CreateTemplateRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateTemplateFromProjectUseCase {

    private final TemplateCommandPort templateCommandPort;

    public ProjectTemplate execute(Long projectId, CreateTemplateRequestDTO request) {
        return templateCommandPort.createTemplateFromProject(projectId, request);
    }
}

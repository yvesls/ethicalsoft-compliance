package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.CreateTemplateRequestDTO;

public interface TemplateCommandPort {
    ProjectTemplate createTemplateFromProject(Long projectId, CreateTemplateRequestDTO request);
}


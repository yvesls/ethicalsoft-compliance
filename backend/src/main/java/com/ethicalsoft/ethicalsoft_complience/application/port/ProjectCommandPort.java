package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectResponseDTO;

public interface ProjectCommandPort {
    ProjectResponseDTO createProject(ProjectCreationRequestDTO request);
}


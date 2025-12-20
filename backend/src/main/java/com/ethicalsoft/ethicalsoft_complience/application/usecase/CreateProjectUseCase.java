package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectCommandPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateProjectUseCase {

    private final ProjectCommandPort projectCommandPort;

    public ProjectResponseDTO execute(ProjectCreationRequestDTO request) {
        return projectCommandPort.createProject(request);
    }
}

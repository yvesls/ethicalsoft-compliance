package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQueryPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectDetailResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectByIdUseCase {

    private final ProjectQueryPort projectQueryPort;

    public ProjectDetailResponseDTO execute(Long projectId) {
        return projectQueryPort.getProjectDetail(projectId);
    }
}

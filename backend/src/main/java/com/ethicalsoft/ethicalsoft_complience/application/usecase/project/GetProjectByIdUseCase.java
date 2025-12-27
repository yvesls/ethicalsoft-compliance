package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectDetailResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQueryPort;
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

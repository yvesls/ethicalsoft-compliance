package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchProjectsUseCase {

    private final ProjectQueryPort projectQueryPort;

    public Page<ProjectSummaryResponseDTO> execute(ProjectSearchRequestDTO filters, Pageable pageable) {
        return projectQueryPort.search(filters, pageable);
    }
}

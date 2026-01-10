package com.ethicalsoft.ethicalsoft_complience.application.usecase.timeline;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.ProjectRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectTimelineStatusPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshAllProjectsTimelineStatusUseCase {

    private final ProjectRepositoryPort projectRepositoryPort;
    private final ProjectTimelineStatusPolicy projectTimelineStatusPolicy;

    @Transactional
    public void execute() {
        var projects = projectRepositoryPort.findAllByOrderByIdAsc();
        for (Project project : projects) {
            try {
                projectTimelineStatusPolicy.updateProjectTimeline(project);
                projectRepositoryPort.save(project);
            } catch (Exception ex) {
                log.error("[timeline] Falha ao atualizar timeline do projeto id={}", project != null ? project.getId() : null, ex);
            }
        }
    }
}

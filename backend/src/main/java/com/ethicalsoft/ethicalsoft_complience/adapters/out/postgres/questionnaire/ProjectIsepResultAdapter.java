package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.ProjectIsepResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectIsepResultRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.ProjectIsepResultCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.ProjectIsepResultQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectIsepResultAdapter implements ProjectIsepResultCommandPort, ProjectIsepResultQueryPort {

    private final ProjectIsepResultRepository repository;

    @Override
    @Transactional
    public ProjectIsepResult save(ProjectIsepResult result) {
        log.info("[project-isep-result-adapter] Persistindo resultado ISEP do projeto id={}", result.getProjectId());
        repository.findByProjectId(result.getProjectId()).ifPresent(existing -> {
            repository.delete(existing);
            repository.flush();
        });
        ProjectIsepResult saved = repository.save(result);
        log.info("[project-isep-result-adapter] Resultado ISEP do projeto persistido id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectIsepResult> findByProjectId(Long projectId) {
        return repository.findByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByProjectId(Long projectId) {
        return repository.existsByProjectId(projectId);
    }
}


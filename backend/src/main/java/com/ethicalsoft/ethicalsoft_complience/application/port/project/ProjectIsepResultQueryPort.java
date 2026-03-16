package com.ethicalsoft.ethicalsoft_complience.application.port.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.ProjectIsepResult;

import java.util.Optional;

public interface ProjectIsepResultQueryPort {

    Optional<ProjectIsepResult> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);
}


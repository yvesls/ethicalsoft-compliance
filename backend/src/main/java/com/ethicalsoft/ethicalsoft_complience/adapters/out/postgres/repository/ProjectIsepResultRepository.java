package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.ProjectIsepResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectIsepResultRepository extends JpaRepository<ProjectIsepResult, Long> {

    Optional<ProjectIsepResult> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);
}


package com.ethicalsoft.ethicalsoft_complience.domain.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface ProjectRepositoryPort {

    Page<Project> findAll(Specification<Project> spec, Pageable pageable);

    List<Project> findAllByOrderByIdAsc();

    Optional<Project> findByIdWithDetails(Long id);

    boolean existsByIdAndOwnerId(Long projectId, Long ownerId);

    Optional<Project> findById(Long id);
}


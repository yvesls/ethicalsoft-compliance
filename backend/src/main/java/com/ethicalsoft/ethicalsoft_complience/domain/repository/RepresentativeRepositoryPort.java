package com.ethicalsoft.ethicalsoft_complience.domain.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Representative;

import java.util.Optional;

public interface RepresentativeRepositoryPort {

    Optional<Representative> findByUserEmailAndProjectId(String email, Long projectId);

    Optional<Representative> findByUserIdAndProjectId(Long userId, Long projectId);

    boolean existsByUserIdAndProjectId(Long userId, Long projectId);

    Optional<Representative> findById(Long id);
}

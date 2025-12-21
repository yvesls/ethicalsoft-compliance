package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres;

import com.ethicalsoft.ethicalsoft_complience.domain.repository.ProjectRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectRepositoryAdapter implements ProjectRepositoryPort {

    private final ProjectRepository delegate;

    @Override
    public Page<Project> findAll(Specification<Project> spec, Pageable pageable) {
        return delegate.findAll(spec, pageable);
    }

    @Override
    public List<Project> findAllByOrderByIdAsc() {
        return delegate.findAllByOrderByIdAsc();
    }

    @Override
    public Optional<Project> findByIdWithDetails(Long id) {
        return delegate.findByIdWithDetails(id);
    }

    @Override
    public boolean existsByIdAndOwnerId(Long projectId, Long ownerId) {
        return delegate.existsByIdAndOwnerId(projectId, ownerId);
    }

    @Override
    public Optional<Project> findById(Long id) {
        return delegate.findById(id);
    }
}


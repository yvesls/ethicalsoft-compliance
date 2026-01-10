package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.RepresentativeRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RepresentativeRepositoryAdapter implements RepresentativeRepositoryPort {

    private final RepresentativeRepository delegate;

    @Override
    public Optional<Representative> findByUserEmailAndProjectId(String email, Long projectId) {
        return delegate.findByUserEmailAndProjectId(email, projectId);
    }

    @Override
    public Optional<Representative> findByUserIdAndProjectId(Long userId, Long projectId) {
        return delegate.findByUserIdAndProjectId(userId, projectId);
    }

    @Override
    public boolean existsByUserIdAndProjectId(Long userId, Long projectId) {
        return delegate.existsByUserIdAndProjectId(userId, projectId);
    }

    @Override
    public Optional<Representative> findById(Long id) {
        return delegate.findById(id);
    }
}

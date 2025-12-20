package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres;

import com.ethicalsoft.ethicalsoft_complience.domain.repository.UserRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserRepository delegate;

    @Override
    public Optional<User> findByEmail(String email) {
        return delegate.findByEmail(email);
    }

    @Override
    public Optional<User> findWithRepresentativesByEmail(String email) {
        return delegate.findWithRepresentativesByEmail(email);
    }

    @Override
    public Optional<User> findWithOwnedProjectsByEmail(String email) {
        return delegate.findWithOwnedProjectsByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return delegate.findById(id);
    }
}


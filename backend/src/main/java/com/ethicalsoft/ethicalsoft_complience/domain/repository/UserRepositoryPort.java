package com.ethicalsoft.ethicalsoft_complience.domain.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByEmail(String email);

    Optional<User> findWithRepresentativesByEmail(String email);

    Optional<User> findWithOwnedProjectsByEmail(String email);

    Optional<User> findById(Long id);
}


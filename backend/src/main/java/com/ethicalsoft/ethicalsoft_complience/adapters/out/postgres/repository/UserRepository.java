package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("select u from User u left join fetch u.representatives r left join fetch r.roles where u.email = :email")
    Optional<User> findWithRepresentativesByEmail(String email);

    @Query("select distinct u from User u left join fetch u.projects where u.email = :email")
    Optional<User> findWithOwnedProjectsByEmail(String email);
}

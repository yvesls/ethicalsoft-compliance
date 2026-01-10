package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RepresentativeRepository extends JpaRepository<Representative, Long> {

    @Query("select r from Representative r where r.user.email = :email and r.project.id = :projectId")
    Optional<Representative> findByUserEmailAndProjectId(String email, Long projectId);

    @Query("select r from Representative r where r.user.id = :userId and r.project.id = :projectId")
    Optional<Representative> findByUserIdAndProjectId(Long userId, Long projectId);

    boolean existsByUserIdAndProjectId(Long userId, Long projectId);
}


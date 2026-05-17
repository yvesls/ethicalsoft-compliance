package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RepresentativeRepository extends JpaRepository<Representative, Long> {

    @Query("select distinct r from Representative r left join fetch r.roles where r.project.id = :projectId and r.deletionDate is null")
    List<Representative> findByProjectId(Long projectId);

    @Query("select distinct r from Representative r left join fetch r.roles where r.project.id = :projectId")
    List<Representative> findAllByProjectIdIncludingDeleted(Long projectId);

    @Query("select distinct r from Representative r left join fetch r.roles where r.user.email = :email and r.project.id = :projectId and r.deletionDate is null")
    Optional<Representative> findByUserEmailAndProjectId(String email, Long projectId);

    @Query("select distinct r from Representative r left join fetch r.roles where r.user.id = :userId and r.project.id = :projectId and r.deletionDate is null")
    Optional<Representative> findByUserIdAndProjectId(Long userId, Long projectId);

    @Query("select count(r) > 0 from Representative r where r.user.id = :userId and r.project.id = :projectId and r.deletionDate is null")
    boolean existsByUserIdAndProjectId(Long userId, Long projectId);

    @Query("select distinct r from Representative r left join fetch r.roles where r.id = :id")
    Optional<Representative> findById(Long id);
}

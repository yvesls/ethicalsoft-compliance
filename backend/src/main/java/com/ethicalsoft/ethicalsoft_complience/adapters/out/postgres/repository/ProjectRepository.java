package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    List<Project> findAllByOrderByIdAsc();

    boolean existsByIdAndOwnerId(Long projectId, Long ownerId);

    @Query("select distinct p from Project p " +
            "left join fetch p.stages " +
            "left join fetch p.iterations " +
            "left join fetch p.representatives r " +
            "left join fetch r.user " +
            "left join fetch r.roles " +
            "left join fetch p.questionnaires q " +
            "left join fetch q.stage " +
            "left join fetch q.iterationRef " +
            "left join fetch q.questions " +
            "where p.id = :id")
    Optional<Project> findByIdWithDetails(@Param("id") Long id);

    @Override
    List<Project> findAll(Specification<Project> spec);

    @Query("select distinct p from Project p left join fetch p.representatives r left join fetch r.user " +
            "where p.deadline is not null and p.deadline between :from and :to and p.status <> com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum.ENCERRADO")
    List<Project> findWithDeadlineBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

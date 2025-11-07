package com.ethicalsoft.ethicalsoft_complience.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

	@EntityGraph(attributePaths = {
			"representatives",
			"stages",
			"iterations",
			"questionnaires",
			"questionnaires.stage"
	})
	Page<Project> findAll( Specification<Project> spec, Pageable pageable);

	@Query(value = "SELECT DISTINCT p FROM Project p " +
			"LEFT JOIN FETCH p.representatives " +
			"LEFT JOIN FETCH p.stages " +
			"LEFT JOIN FETCH p.iterations " +
			"LEFT JOIN FETCH p.questionnaires q " +
			"LEFT JOIN FETCH q.stage",
			countQuery = "SELECT count(DISTINCT p) FROM Project p")
	Page<Project> findAllWithCollections(Pageable pageable);
}
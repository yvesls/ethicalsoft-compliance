package com.ethicalsoft.ethicalsoft_complience.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

	@EntityGraph(attributePaths = {
			"representatives",
			"representatives.user",
			"stages",
			"iterations",
			"questionnaires",
			"questionnaires.stage",
			"questionnaires.iterationRef",
			"questionnaires.questions"
	})
	Page<Project> findAll( Specification<Project> spec, Pageable pageable);

	@EntityGraph(attributePaths = {
			"representatives",
			"representatives.user",
			"stages",
			"iterations",
			"questionnaires",
			"questionnaires.stage",
			"questionnaires.iterationRef",
			"questionnaires.questions"
	})
	List<Project> findAllByOrderByIdAsc();

	@EntityGraph(attributePaths = {
			"representatives",
			"representatives.user",
			"stages",
			"iterations",
			"questionnaires",
			"questionnaires.stage",
			"questionnaires.iterationRef",
			"questionnaires.questions"
	})
	@Query("select p from Project p where p.id = :id")
	Optional<Project> findByIdWithDetails( Long id );

	boolean existsByIdAndOwnerId(Long projectId, Long ownerId);
}
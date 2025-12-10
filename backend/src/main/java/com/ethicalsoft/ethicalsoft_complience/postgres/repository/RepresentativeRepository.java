package com.ethicalsoft.ethicalsoft_complience.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Representative;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepresentativeRepository extends JpaRepository<Representative, Long> {

	@EntityGraph(attributePaths = "roles")
	Optional<Representative> findByUserEmailAndProjectId( String email, Long projectId );

}

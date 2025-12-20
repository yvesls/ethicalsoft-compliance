package com.ethicalsoft.ethicalsoft_complience.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail( String email );

	@EntityGraph(attributePaths = {"representatives", "representatives.project", "representatives.roles"})
	Optional<User> findWithRepresentativesByEmail(String email);

	@EntityGraph(attributePaths = {"projects"})
	Optional<User> findWithOwnedProjectsByEmail(String email);
}

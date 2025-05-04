package com.ethicalsoft.ethicalsoft_complience.repository;

import com.ethicalsoft.ethicalsoft_complience.model.Representative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepresentativeRepository extends JpaRepository<Representative, Long> {

	Optional<Representative> findByUserEmailAndProjectId( String email, Long projectId );

}

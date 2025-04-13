package com.ethicalsoft.ethicalsoft_complience.repository;

import com.ethicalsoft.ethicalsoft_complience.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail( String email );

}

package com.ethicalsoft.ethicalsoft_complience.repository;

import com.ethicalsoft.ethicalsoft_complience.model.RefreshToken;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash( String tokenHash );

	void deleteByTokenHash( String tokenHash );

	void deleteByUser( User user );

}
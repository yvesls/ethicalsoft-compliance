package com.ethicalsoft.ethicalsoft_complience.repository;

import com.ethicalsoft.ethicalsoft_complience.model.RecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {

	Optional<RecoveryCode> findByEmailAndCodeAndExpirationAfter( String email, String code, LocalDateTime now );

	void deleteAllByEmail( String email );
}

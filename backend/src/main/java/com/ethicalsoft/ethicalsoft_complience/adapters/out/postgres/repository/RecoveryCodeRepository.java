package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.RecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {

    Optional<RecoveryCode> findByEmailAndCodeAndExpirationAfter(String email, String code, LocalDateTime now);

    void deleteAllByEmail(String email);
}


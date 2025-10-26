package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.UserNotFoundException;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.RecoveryCode;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth.CodeValidationDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth.PasswordRecoveryDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth.PasswordResetDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RecoveryCodeRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RecoveryCodeRepository recoveryCodeRepository;
	private final EmailService emailService;

	@Transactional( rollbackOn = Exception.class )
	public void requestRecovery( PasswordRecoveryDTO passwordRecoveryDTO ) {
		userRepository.findByEmail( passwordRecoveryDTO.getEmail() ).orElseThrow( () -> new UsernameNotFoundException( "User not found" ) );

		String code = UUID.randomUUID().toString().substring( 0, 6 );
		RecoveryCode recoveryCode = new RecoveryCode( passwordRecoveryDTO.getEmail(), code, LocalDateTime.now().plusMinutes( 10 ) );

		recoveryCodeRepository.save( recoveryCode );
		emailService.sendRecoveryEmail( passwordRecoveryDTO.getEmail(), code );
	}

	@Transactional( rollbackOn = Exception.class )
	public void validateCode( CodeValidationDTO codeValidationDTO ) {
		recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter( codeValidationDTO.getEmail(), codeValidationDTO.getCode(), LocalDateTime.now() ).orElseThrow( () -> new IllegalArgumentException( "Invalid or expired code." ) );

		recoveryCodeRepository.deleteAllByEmail( codeValidationDTO.getEmail() );
	}

	@Transactional( rollbackOn = Exception.class )
	public void resetPassword( PasswordResetDTO passwordResetDTO ) {
		var user = userRepository.findByEmail( passwordResetDTO.getEmail() ).orElseThrow(
				() -> new UserNotFoundException( "User not found" )
		);

		if ( passwordEncoder.matches( passwordResetDTO.getNewPassword(), user.getPassword() ) ) {
			throw new IllegalArgumentException( "New password cannot be the same as the old password." );
		}

		user.setPassword( passwordEncoder.encode( passwordResetDTO.getNewPassword() ) );
		userRepository.save( user );
	}
}

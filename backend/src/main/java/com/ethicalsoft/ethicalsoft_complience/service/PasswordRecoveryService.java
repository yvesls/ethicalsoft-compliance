package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.model.RecoveryCode;
import com.ethicalsoft.ethicalsoft_complience.model.dto.CodeValidationDTO;
import com.ethicalsoft.ethicalsoft_complience.model.dto.PasswordRecoveryDTO;
import com.ethicalsoft.ethicalsoft_complience.model.dto.PasswordResetDTO;
import com.ethicalsoft.ethicalsoft_complience.repository.RecoveryCodeRepository;
import com.ethicalsoft.ethicalsoft_complience.repository.UserRepository;
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

	public void validateCode( CodeValidationDTO codeValidationDTO ) {
		var isValid = recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter( codeValidationDTO.getEmail(), codeValidationDTO.getCode(), LocalDateTime.now() ).isPresent();
		if ( !isValid ) {
			throw new IllegalArgumentException( "Invalid or expired code." );
		}
		recoveryCodeRepository.deleteByEmail( codeValidationDTO.getEmail() );
	}

	@Transactional( rollbackOn = Exception.class )
	public void resetPassword( PasswordResetDTO passwordResetDTO ) {

		var user = userRepository.findByEmail( passwordResetDTO.getEmail() ).orElseThrow( () -> new UsernameNotFoundException( "User not found" ) );

		user.setPassword( passwordEncoder.encode( passwordResetDTO.getNewPassword() ) );
		userRepository.save( user );
	}
}

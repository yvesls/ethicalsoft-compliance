package com.ethicalsoft.ethicalsoft_complience.service;
import com.ethicalsoft.ethicalsoft_complience.application.port.NotificationDispatcherPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.PasswordRecoveryPort;
import com.ethicalsoft.ethicalsoft_complience.exception.UserNotFoundException;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.RecoveryCode;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.CodeValidationDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.PasswordRecoveryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.PasswordResetDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RecoveryCodeRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryService implements PasswordRecoveryPort {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final NotificationDispatcherPort notificationDispatcher;

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void requestRecovery(PasswordRecoveryDTO passwordRecoveryDTO) {
        try {
            log.info("[password-recovery] Solicitação de recuperação para email={}", passwordRecoveryDTO != null ? passwordRecoveryDTO.getEmail() : null);
            userRepository.findByEmail( Objects.requireNonNull(passwordRecoveryDTO).getEmail() ).orElseThrow( () -> new UsernameNotFoundException( "User not found" ) );

            String code = UUID.randomUUID().toString().substring( 0, 6 );
            RecoveryCode recoveryCode = new RecoveryCode( passwordRecoveryDTO.getEmail(), code, LocalDateTime.now().plusMinutes( 10 ) );

            recoveryCodeRepository.save( recoveryCode );
            notificationDispatcher.dispatchRecoveryCode( passwordRecoveryDTO.getEmail(), code );
            log.info("[password-recovery] Código de recuperação gerado e notificação despachada para {}", passwordRecoveryDTO.getEmail());
        } catch ( Exception ex ) {
            log.error("[password-recovery] Falha ao solicitar recuperação para {}", passwordRecoveryDTO != null ? passwordRecoveryDTO.getEmail() : null, ex);
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void validateCode(CodeValidationDTO codeValidationDTO) {
        try {
            log.info("[password-recovery] Validando código para email={}", codeValidationDTO != null ? codeValidationDTO.getEmail() : null);
            recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter( Objects.requireNonNull(codeValidationDTO).getEmail(), codeValidationDTO.getCode(), LocalDateTime.now() ).orElseThrow( () -> new IllegalArgumentException( "Invalid or expired code." ) );

            recoveryCodeRepository.deleteAllByEmail( codeValidationDTO.getEmail() );
            log.info("[password-recovery] Código validado e removido para {}", codeValidationDTO.getEmail());
        } catch ( Exception ex ) {
            log.error("[password-recovery] Falha ao validar código para {}", codeValidationDTO != null ? codeValidationDTO.getEmail() : null, ex);
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void resetPassword(PasswordResetDTO passwordResetDTO) {
        try {
            log.info("[password-recovery] Alterando senha para email={}", passwordResetDTO != null ? passwordResetDTO.getEmail() : null);
            var user = userRepository.findByEmail( Objects.requireNonNull(passwordResetDTO).getEmail() ).orElseThrow(
                    () -> new UserNotFoundException( "User not found" )
            );

            if ( passwordEncoder.matches( passwordResetDTO.getNewPassword(), user.getPassword() ) ) {
                throw new IllegalArgumentException( "New password cannot be the same as the old password." );
            }

            user.setPassword( passwordEncoder.encode( passwordResetDTO.getNewPassword() ) );

            if ( passwordResetDTO.isFirstAccessFlow() ) {
                user.setFirstAccess( false );
            }

            userRepository.save( user );
            log.info("[password-recovery] Senha redefinida para usuário id={}", user.getId());
        } catch ( Exception ex ) {
            log.error("[password-recovery] Falha ao redefinir senha para {}", passwordResetDTO != null ? passwordResetDTO.getEmail() : null, ex);
            throw ex;
        }
    }
}
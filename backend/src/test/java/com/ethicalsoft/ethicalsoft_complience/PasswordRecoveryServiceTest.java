package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.application.port.NotificationDispatcherPort;
import com.ethicalsoft.ethicalsoft_complience.exception.UserNotFoundException;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.RecoveryCode;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.CodeValidationDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.PasswordRecoveryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.PasswordResetDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RecoveryCodeRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import com.ethicalsoft.ethicalsoft_complience.service.PasswordRecoveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RecoveryCodeRepository recoveryCodeRepository;
    @Mock
    private NotificationDispatcherPort notificationDispatcher;
    @InjectMocks
    private PasswordRecoveryService passwordRecoveryService;

    @Test
    void requestRecovery_success() {
        User user = new User();
        user.setEmail("test@example.com");
        PasswordRecoveryDTO passwordRecoveryDTO = new PasswordRecoveryDTO();
        passwordRecoveryDTO.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        passwordRecoveryService.requestRecovery(passwordRecoveryDTO);

        verify(recoveryCodeRepository, times(1)).save(any(RecoveryCode.class));
        verify(notificationDispatcher, times(1)).dispatchRecoveryCode(anyString(), anyString());
    }

    @Test
    void requestRecovery_userNotFound() {
        when(userRepository.findByEmail("invalid@example.com")).thenReturn(Optional.empty());
        PasswordRecoveryDTO passwordRecoveryDTO = new PasswordRecoveryDTO();
        passwordRecoveryDTO.setEmail("invalid@example.com");
        assertThrows(UsernameNotFoundException.class, () -> passwordRecoveryService.requestRecovery(passwordRecoveryDTO));
    }

    @Test
    void validateCode_success() {
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(10);
        RecoveryCode code = new RecoveryCode("test@example.com", "123456", expirationTime);
        CodeValidationDTO codeValidationDTO = new CodeValidationDTO();
        codeValidationDTO.setEmail("test@example.com");
        codeValidationDTO.setCode("123456");

        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq("test@example.com"), eq("123456"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(code));

        assertDoesNotThrow(() -> passwordRecoveryService.validateCode(codeValidationDTO));
    }

    @Test
    void validateCode_invalidCode() {
        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq("test@example.com"), eq("invalid"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        CodeValidationDTO codeValidationDTO = new CodeValidationDTO();
        codeValidationDTO.setEmail("test@example.com");
        codeValidationDTO.setCode("invalid");
        assertThrows(IllegalArgumentException.class, () -> passwordRecoveryService.validateCode(codeValidationDTO));
    }

    @Test
    void validateCode_expiredCode() {
        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq("test@example.com"), eq("123456"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        CodeValidationDTO codeValidationDTO = new CodeValidationDTO();
        codeValidationDTO.setEmail("test@example.com");
        codeValidationDTO.setCode("123456");
        assertThrows(IllegalArgumentException.class, () -> passwordRecoveryService.validateCode(codeValidationDTO));
    }

    @Test
    void validateCode_nonexistentEmail() {
        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq("nonexistent@example.com"), eq("123456"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        CodeValidationDTO codeValidationDTO = new CodeValidationDTO();
        codeValidationDTO.setEmail("nonexistent@example.com");
        codeValidationDTO.setCode("123456");
        assertThrows(IllegalArgumentException.class, () -> passwordRecoveryService.validateCode(codeValidationDTO));
    }

    @Test
    void resetPassword_success() {
        String email = "test@example.com";
        String newPassword = "newPassword123";
        User user = new User();
        user.setEmail(email);
        PasswordResetDTO passwordResetDTO = new PasswordResetDTO();
        passwordResetDTO.setEmail(email);
        passwordResetDTO.setNewPassword(newPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");

        passwordRecoveryService.resetPassword(passwordResetDTO);

        verify(userRepository).save(user);
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    void resetPassword_userNotFound() {
        String email = "nonexistent@example.com";
        String newPassword = "newPassword123";
        PasswordResetDTO passwordResetDTO = new PasswordResetDTO();
        passwordResetDTO.setEmail(email);
        passwordResetDTO.setNewPassword(newPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> passwordRecoveryService.resetPassword(passwordResetDTO));
    }
}
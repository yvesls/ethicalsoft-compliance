package com.ethicalsoft.ethicalsoft_complience;
import com.ethicalsoft.ethicalsoft_complience.model.RecoveryCode;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.repository.RecoveryCodeRepository;
import com.ethicalsoft.ethicalsoft_complience.repository.UserRepository;
import com.ethicalsoft.ethicalsoft_complience.service.EmailService;
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

import static org.junit.jupiter.api.Assertions.*;
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
    private EmailService emailService;
    @InjectMocks
    private PasswordRecoveryService passwordRecoveryService;

    @Test
    void requestRecovery_success() {
        User user = new User();
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        doNothing().when(emailService).sendRecoveryEmail(anyString(), anyString());

        passwordRecoveryService.requestRecovery("test@example.com");

        verify(recoveryCodeRepository, times(1)).save(any(RecoveryCode.class));
        verify(emailService, times(1)).sendRecoveryEmail(anyString(), anyString());
    }

    @Test
    void requestRecovery_userNotFound() {
        when(userRepository.findByEmail("invalid@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> passwordRecoveryService.requestRecovery("invalid@example.com"));
    }

    @Test
    void validateCode_success() {
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(10);
        RecoveryCode code = new RecoveryCode("test@example.com", "123456", expirationTime);

        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq("test@example.com"), eq("123456"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(code));

        assertDoesNotThrow(() -> passwordRecoveryService.validateCode("test@example.com", "123456"));
    }

    @Test
    void validateCode_invalidCode() {
        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq("test@example.com"), eq("invalid"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> passwordRecoveryService.validateCode("test@example.com", "invalid"));
    }

    @Test
    void validateCode_expiredCode() {
        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq("test@example.com"), eq("123456"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> passwordRecoveryService.validateCode("test@example.com", "123456"));
    }

    @Test
    void validateCode_nonexistentEmail() {
        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq("nonexistent@example.com"), eq("123456"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> passwordRecoveryService.validateCode("nonexistent@example.com", "123456"));
    }

    @Test
    void resetPassword_success() {
        String email = "test@example.com";
        String code = "123456";
        String newPassword = "newPassword123";
        User user = new User();
        user.setEmail(email);

        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq(email), eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new RecoveryCode(email, code, LocalDateTime.now().plusMinutes(10))));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");

        passwordRecoveryService.resetPassword(email, code, newPassword);

        verify(userRepository).save(user);
        verify(recoveryCodeRepository).deleteByEmail(email);
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    void resetPassword_invalidCode() {
        String email = "test@example.com";
        String code = "invalid";
        String newPassword = "newPassword123";

        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq(email), eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> passwordRecoveryService.resetPassword(email, code, newPassword));

        verify(userRepository, never()).save(any());
        verify(recoveryCodeRepository, never()).deleteByEmail(any());
    }

    @Test
    void resetPassword_userNotFound() {
        String email = "nonexistent@example.com";
        String code = "123456";
        String newPassword = "newPassword123";

        when(recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(
                eq(email), eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new RecoveryCode(email, code, LocalDateTime.now().plusMinutes(10))));

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> passwordRecoveryService.resetPassword(email, code, newPassword));
    }
}
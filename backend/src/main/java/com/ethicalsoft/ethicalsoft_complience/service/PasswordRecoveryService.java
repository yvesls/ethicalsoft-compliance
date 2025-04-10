package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.model.RecoveryCode;
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

    @Transactional(rollbackOn = Exception.class)
    public void requestRecovery(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String code = UUID.randomUUID().toString().substring(0, 6);
        RecoveryCode recoveryCode = new RecoveryCode(email, code, LocalDateTime.now().plusMinutes(10));

        recoveryCodeRepository.save(recoveryCode);
        emailService.sendRecoveryEmail(email, code);
    }

    public void validateCode(String email, String code) {
        var isValid = recoveryCodeRepository.findByEmailAndCodeAndExpirationAfter(email, code, LocalDateTime.now()).isPresent();
        if (!isValid) {
            throw new IllegalArgumentException("Invalid or expired code.");
        }
    }

    @Transactional(rollbackOn = Exception.class)
    public void resetPassword(String email, String newPassword) {

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        recoveryCodeRepository.deleteByEmail(email);
    }
}

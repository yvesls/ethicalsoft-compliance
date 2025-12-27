package com.ethicalsoft.ethicalsoft_complience.application.usecase.auth;

import com.ethicalsoft.ethicalsoft_complience.application.port.PasswordRecoveryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.PasswordResetDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final PasswordRecoveryPort recoveryPort;

    public void execute(PasswordResetDTO dto) {
        recoveryPort.resetPassword(dto);
    }
}

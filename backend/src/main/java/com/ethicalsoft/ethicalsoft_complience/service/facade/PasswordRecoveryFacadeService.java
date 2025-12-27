package com.ethicalsoft.ethicalsoft_complience.service.facade;

import com.ethicalsoft.ethicalsoft_complience.application.port.PasswordRecoveryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.CodeValidationDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.PasswordRecoveryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.PasswordResetDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryFacadeService implements PasswordRecoveryPort {
    private final PasswordRecoveryPort recoveryService;

    @Override
    public void requestRecovery(PasswordRecoveryDTO dto) {
        recoveryService.requestRecovery(dto);
    }

    @Override
    public void validateCode(CodeValidationDTO dto) {
        recoveryService.validateCode(dto);
    }

    @Override
    public void resetPassword(PasswordResetDTO dto) {
        recoveryService.resetPassword(dto);
    }
}

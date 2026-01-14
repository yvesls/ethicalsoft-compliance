package com.ethicalsoft.ethicalsoft_complience.application.usecase.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.CodeValidationDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.PasswordRecoveryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidateCodeUseCase {

    private final PasswordRecoveryPort recoveryPort;

    public void execute(CodeValidationDTO dto) {
        recoveryPort.validateCode(dto);
    }
}

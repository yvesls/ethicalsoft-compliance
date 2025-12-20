package com.ethicalsoft.ethicalsoft_complience.application.usecase.auth;

import com.ethicalsoft.ethicalsoft_complience.application.port.PasswordRecoveryPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth.PasswordRecoveryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestRecoveryUseCase {

    private final PasswordRecoveryPort recoveryPort;

    public void execute(PasswordRecoveryDTO dto) {
        recoveryPort.requestRecovery(dto);
    }
}

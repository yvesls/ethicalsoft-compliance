package com.ethicalsoft.ethicalsoft_complience.application.port.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.CodeValidationDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.PasswordRecoveryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.PasswordResetDTO;

public interface PasswordRecoveryPort {
    void requestRecovery(PasswordRecoveryDTO dto);
    void validateCode(CodeValidationDTO dto);
    void resetPassword(PasswordResetDTO dto);
}
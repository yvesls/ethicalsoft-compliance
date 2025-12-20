package com.ethicalsoft.ethicalsoft_complience.application.usecase.auth;

import com.ethicalsoft.ethicalsoft_complience.application.port.AuthCommandPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth.AuthDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth.RefreshTokenDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final AuthCommandPort authCommandPort;

    public AuthDTO execute(RefreshTokenDTO dto) {
        return authCommandPort.refresh(dto);
    }
}

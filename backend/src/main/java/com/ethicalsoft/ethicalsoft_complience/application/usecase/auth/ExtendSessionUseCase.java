package com.ethicalsoft.ethicalsoft_complience.application.usecase.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.ExtendSessionDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.ExtendSessionResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.AuthCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExtendSessionUseCase {

    private final AuthCommandPort authCommandPort;

    public ExtendSessionResponseDTO execute(ExtendSessionDTO dto) {
        return authCommandPort.extendSession(dto);
    }
}

package com.ethicalsoft.ethicalsoft_complience.application.usecase.auth;

import com.ethicalsoft.ethicalsoft_complience.application.port.AuthCommandPort;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth.RegisterUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterUseCase {

    private final AuthCommandPort authCommandPort;

    public void execute(RegisterUserDTO dto) {
        authCommandPort.register(dto);
    }
}

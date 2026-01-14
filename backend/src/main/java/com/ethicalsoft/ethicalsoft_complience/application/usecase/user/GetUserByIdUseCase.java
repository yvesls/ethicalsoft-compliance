package com.ethicalsoft.ethicalsoft_complience.application.usecase.user;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.UserDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetUserByIdUseCase {

    private final UserQueryPort userQueryPort;

    public UserDTO execute(Long id) {
        return userQueryPort.findById(id);
    }
}

package com.ethicalsoft.ethicalsoft_complience.application.usecase.user;

import com.ethicalsoft.ethicalsoft_complience.application.port.UserQueryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserQueryPort userQueryPort;

    public Page<UserDTO> execute(Pageable pageable) {
        return userQueryPort.findAll(pageable);
    }
}

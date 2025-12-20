package com.ethicalsoft.ethicalsoft_complience.application.usecase.user;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.UserDTO;
import com.ethicalsoft.ethicalsoft_complience.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetUserByIdUseCase {

    private final UserService userService;

    public UserDTO execute(Long id) {
        return userService.findById(id);
    }
}


package com.ethicalsoft.ethicalsoft_complience.application.usecase.user;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.UserDTO;
import com.ethicalsoft.ethicalsoft_complience.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserService userService;

    public Page<UserDTO> execute(Pageable pageable) {
        return userService.findAll(pageable);
    }
}


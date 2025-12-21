package com.ethicalsoft.ethicalsoft_complience.adapters.security;

import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserAdapter implements CurrentUserPort {

    private final AuthService authService;

    @Override
    public User getCurrentUser() {
        return authService.getAuthenticatedUser();
    }
}


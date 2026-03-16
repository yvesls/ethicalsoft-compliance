package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.auth.AuthAdapter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.AuthenticatedUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.CurrentUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserAdapter implements CurrentUserPort, AuthenticatedUserPort {

    private final AuthAdapter authAdapter;

    @Override
    public User getCurrentUser() {
        return authAdapter.getAuthenticatedUser();
    }

    @Override
    public User getAuthenticatedUser() {
        return authAdapter.getAuthenticatedUser();
    }

    @Override
    public Long getAuthenticatedUserId() {
        return authAdapter.getAuthenticatedUserId();
    }
}

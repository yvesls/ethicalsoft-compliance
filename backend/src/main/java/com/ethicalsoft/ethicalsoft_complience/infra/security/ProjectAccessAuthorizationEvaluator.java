package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.UserRoleEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAccessAuthorizationEvaluator {

    public boolean canAccess(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream().anyMatch(this::isAllowedRole);
    }

    private boolean isAllowedRole(GrantedAuthority authority) {
        String name = authority.getAuthority();
        if (name == null) {
            return false;
        }

        if (UserRoleEnum.ADMIN.name().equals(name)) {
            return true;
        }

        return name.startsWith("ROLE_");
    }
}

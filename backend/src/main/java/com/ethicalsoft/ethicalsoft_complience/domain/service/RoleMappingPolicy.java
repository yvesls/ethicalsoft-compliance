package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleMappingPolicy {

    public Set<Role> mapRoles(Set<Long> desiredRoleIds, Map<Long, Role> resolvedRoles) {
        if (desiredRoleIds == null || desiredRoleIds.isEmpty()) {
            return Set.of();
        }
        return desiredRoleIds.stream()
                .map(resolvedRoles::get)
                .collect(Collectors.toSet());
    }
}


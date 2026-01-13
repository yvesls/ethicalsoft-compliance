package com.ethicalsoft.ethicalsoft_complience.domain.notification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class NotificationAuthorizationPolicy {

    public void validateCanSend(List<String> whoCanSend, UserRoleEnum currentRole, List<String> currentRolesByName) {
        if (whoCanSend == null || whoCanSend.isEmpty()) {
            return;
        }
        Set<String> allowed = whoCanSend.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (currentRole != null && allowed.contains(currentRole.name().toUpperCase(Locale.ROOT))) {
            return;
        }

        if (currentRolesByName != null) {
            boolean anyMatch = currentRolesByName.stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.toUpperCase(Locale.ROOT))
                    .anyMatch(allowed::contains);
            if (anyMatch) {
                return;
            }
        }

        throw new BusinessException("Usuário não possui permissão para enviar esta notificação");
    }
}


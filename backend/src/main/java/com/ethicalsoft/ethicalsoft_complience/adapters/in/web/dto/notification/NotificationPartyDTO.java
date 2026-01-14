package com.ethicalsoft.ethicalsoft_complience.adapters.in.web.dto.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationParty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record NotificationPartyDTO(
        Long userId,
        String fullName,
        String email,
        List<String> roles
) {
    public static NotificationPartyDTO fromDomain(NotificationParty party) {
        if (party == null) {
            return null;
        }
        List<String> safeRoles = Optional.ofNullable(party.roles()).orElse(Collections.emptyList());
        return new NotificationPartyDTO(party.userId(), party.fullName(), party.email(), safeRoles);
    }
}


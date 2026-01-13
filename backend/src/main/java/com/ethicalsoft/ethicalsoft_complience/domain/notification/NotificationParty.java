package com.ethicalsoft.ethicalsoft_complience.domain.notification;

import java.util.List;

public record NotificationParty(
        Long userId,
        String fullName,
        String email,
        List<String> roles
) {
}


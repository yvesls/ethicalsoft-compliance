package com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationParty;

import java.util.Map;

public record SendInternalNotificationCommand(
        String templateKey,
        NotificationParty sender,
        NotificationParty recipient,
        Map<String, String> context
) {
}


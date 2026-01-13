package com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;

import java.util.Map;

public record SendNotificationCommand(
        NotificationType type,
        Map<String, Object> context
) {
}

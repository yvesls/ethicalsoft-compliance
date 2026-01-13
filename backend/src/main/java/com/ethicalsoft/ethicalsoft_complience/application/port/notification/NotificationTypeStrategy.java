package com.ethicalsoft.ethicalsoft_complience.application.port.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;

public interface NotificationTypeStrategy {
    NotificationType type();
    void send(SendNotificationCommand command);
}

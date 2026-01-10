package com.ethicalsoft.ethicalsoft_complience.application.port.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.Notification;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationStatus;

import java.util.Optional;

public interface UpdateInternalNotificationStatusPort {
    Optional<Notification> findById(String id);
    Notification save(Notification notification);
}


package com.ethicalsoft.ethicalsoft_complience.application.port.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.Notification;

public interface InternalNotificationPort {
    Notification save(Notification notification);
}


package com.ethicalsoft.ethicalsoft_complience.application.port.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;

import java.util.Optional;

public interface NotificationTemplatePort {
    Optional<NotificationTemplate> findByKey(String key);
}


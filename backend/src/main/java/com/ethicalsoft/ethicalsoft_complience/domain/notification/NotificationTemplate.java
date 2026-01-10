package com.ethicalsoft.ethicalsoft_complience.domain.notification;

import java.util.List;

public record NotificationTemplate(
        String key,
        List<String> whoCanSend,
        List<String> recipients,
        String title,
        String body,
        List<NotificationChannel> channels
) {
}


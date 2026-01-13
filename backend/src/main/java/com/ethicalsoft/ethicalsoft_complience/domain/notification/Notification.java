package com.ethicalsoft.ethicalsoft_complience.domain.notification;

import java.time.LocalDateTime;

public record Notification(
        String id,
        NotificationParty sender,
        NotificationParty recipient,
        String title,
        String content,
        NotificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String templateKey
) {
    public Notification withStatus(NotificationStatus newStatus, LocalDateTime updatedAt) {
        return new Notification(id, sender, recipient, title, content, newStatus, createdAt, updatedAt, templateKey);
    }
}


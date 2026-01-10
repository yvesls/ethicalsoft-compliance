package com.ethicalsoft.ethicalsoft_complience.controller.dto.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.Notification;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationParty;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        String id,
        String title,
        String content,
        NotificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String templateKey,
        NotificationParty sender,
        NotificationParty recipient
) {
    public static NotificationResponseDTO fromDomain(Notification n) {
        return new NotificationResponseDTO(
                n.id(),
                n.title(),
                n.content(),
                n.status(),
                n.createdAt(),
                n.updatedAt(),
                n.templateKey(),
                n.sender(),
                n.recipient()
        );
    }
}


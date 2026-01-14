package com.ethicalsoft.ethicalsoft_complience.adapters.in.web.dto.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.Notification;
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
        NotificationPartyDTO sender,
        NotificationPartyDTO recipient
) {
    public static NotificationResponseDTO fromDomain(Notification n) {
        if (n == null) {
            return null;
        }
        return new NotificationResponseDTO(
                n.id(),
                n.title(),
                n.content(),
                n.status(),
                n.createdAt(),
                n.updatedAt(),
                n.templateKey(),
                NotificationPartyDTO.fromDomain(n.sender()),
                NotificationPartyDTO.fromDomain(n.recipient())
        );
    }
}


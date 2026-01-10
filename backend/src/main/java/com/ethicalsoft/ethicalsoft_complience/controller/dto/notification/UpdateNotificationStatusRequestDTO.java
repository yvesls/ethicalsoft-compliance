package com.ethicalsoft.ethicalsoft_complience.controller.dto.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationStatusRequestDTO(
        @NotNull NotificationStatus status
) {
}


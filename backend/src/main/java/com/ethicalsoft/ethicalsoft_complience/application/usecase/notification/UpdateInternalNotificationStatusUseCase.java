package com.ethicalsoft.ethicalsoft_complience.application.usecase.notification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.UpdateInternalNotificationStatusPort;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationStatus;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UpdateInternalNotificationStatusUseCase {

    private final UpdateInternalNotificationStatusPort port;
    private final CurrentUserPort currentUserPort;

    public void execute(String notificationId, NotificationStatus newStatus) {
        var notification = port.findById(notificationId)
                .orElseThrow(() -> new BusinessException("Notification not found"));

        var currentUser = currentUserPort.getCurrentUser();
        boolean isOwner = notification.recipient() != null && notification.recipient().userId() != null
                && notification.recipient().userId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRoleEnum.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Usuário não autorizado a atualizar o status desta notificação.");
        }

        port.save(notification.withStatus(newStatus, LocalDateTime.now()));
    }
}


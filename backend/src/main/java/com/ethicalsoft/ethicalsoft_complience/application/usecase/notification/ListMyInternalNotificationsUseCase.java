package com.ethicalsoft.ethicalsoft_complience.application.usecase.notification;

import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.ListInternalNotificationsPort;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.notification.NotificationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyInternalNotificationsUseCase {

    private final ListInternalNotificationsPort listInternalNotificationsPort;
    private final CurrentUserPort currentUserPort;

    public List<NotificationResponseDTO> executeOnlyUnseen() {
        Long userId = currentUserPort.getCurrentUser().getId();
        return listInternalNotificationsPort.listUnseenForRecipient(userId)
                .stream()
                .map(NotificationResponseDTO::fromDomain)
                .toList();
    }
}

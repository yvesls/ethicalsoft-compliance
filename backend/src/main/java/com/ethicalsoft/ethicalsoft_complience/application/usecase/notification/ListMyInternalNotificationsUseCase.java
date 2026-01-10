package com.ethicalsoft.ethicalsoft_complience.application.usecase.notification;

import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.ListInternalNotificationsPort;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.notification.NotificationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListMyInternalNotificationsUseCase {

    private final ListInternalNotificationsPort listInternalNotificationsPort;
    private final CurrentUserPort currentUserPort;

    public Page<NotificationResponseDTO> execute(Pageable pageable) {
        Long userId = currentUserPort.getCurrentUser().getId();
        return listInternalNotificationsPort.listForRecipient(userId, pageable)
                .map(NotificationResponseDTO::fromDomain);
    }
}


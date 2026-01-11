package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.ListMyInternalNotificationsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.UpdateInternalNotificationStatusUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.notification.NotificationResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.notification.UpdateNotificationStatusRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ListMyInternalNotificationsUseCase listMyInternalNotificationsUseCase;
    private final UpdateInternalNotificationStatusUseCase updateInternalNotificationStatusUseCase;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    public List<NotificationResponseDTO> listMyNotifications() {
        return listMyInternalNotificationsUseCase.executeOnlyUnseen();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    public void updateStatus(@PathVariable String id, @RequestBody @Valid UpdateNotificationStatusRequestDTO request) {
        updateInternalNotificationStatusUseCase.execute(id, request.status());
    }
}

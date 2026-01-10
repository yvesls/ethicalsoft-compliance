package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.ListMyInternalNotificationsUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.UpdateInternalNotificationStatusUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.notification.NotificationResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.notification.UpdateNotificationStatusRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ListMyInternalNotificationsUseCase listMyInternalNotificationsUseCase;
    private final UpdateInternalNotificationStatusUseCase updateInternalNotificationStatusUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Page<NotificationResponseDTO> listMyNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return listMyInternalNotificationsUseCase.execute(pageable);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public void updateStatus(@PathVariable String id, @RequestBody @Valid UpdateNotificationStatusRequestDTO request) {
        updateInternalNotificationStatusUseCase.execute(id, request.status());
    }
}


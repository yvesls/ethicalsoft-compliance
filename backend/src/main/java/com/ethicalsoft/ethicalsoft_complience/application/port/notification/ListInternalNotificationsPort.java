package com.ethicalsoft.ethicalsoft_complience.application.port.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListInternalNotificationsPort {
    Page<Notification> listForRecipient(Long recipientUserId, Pageable pageable);
}


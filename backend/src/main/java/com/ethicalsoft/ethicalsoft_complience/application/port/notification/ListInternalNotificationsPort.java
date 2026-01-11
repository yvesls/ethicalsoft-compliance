package com.ethicalsoft.ethicalsoft_complience.application.port.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.Notification;

import java.util.List;

public interface ListInternalNotificationsPort {
    List<Notification> listUnseenForRecipient(Long recipientUserId);
}

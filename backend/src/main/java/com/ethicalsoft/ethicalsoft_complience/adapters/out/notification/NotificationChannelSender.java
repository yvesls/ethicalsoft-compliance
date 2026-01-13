package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.port.notification.InternalNotificationPort;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationChannelSender {

    private final InternalNotificationPort internalNotificationPort;
    private final NotificationEmailSender notificationEmailSender;

    public void sendInternal(String templateKey, String titleTemplate, String bodyTemplate, Map<String, String> placeholders, NotificationDispatchRequest request) {
        var title = ResolvePlaceholderPolicy.resolve(titleTemplate, placeholders);
        var body = ResolvePlaceholderPolicy.resolve(bodyTemplate, placeholders);

        Notification notification = new Notification(
                null,
                new NotificationParty(request.getSenderUserId(), request.getSenderName(), request.getSenderEmail(), request.getSenderRoles()),
                new NotificationParty(request.getRecipientUserId(), request.getRecipientName(), request.getRecipientEmail(), request.getRecipientRoles()),
                title,
                body,
                NotificationStatus.UNREAD,
                LocalDateTime.now(),
                null,
                templateKey
        );
        internalNotificationPort.save(notification);
    }

    public void sendEmail(String subjectTemplate, String templateLink, Map<String, String> placeholders, NotificationDispatchRequest request) {
        if (templateLink == null || templateLink.isBlank()) {
            throw new com.ethicalsoft.ethicalsoft_complience.exception.BusinessException("Template de e-mail não configurado para a notificação");
        }
        String subject = ResolvePlaceholderPolicy.resolve(subjectTemplate, placeholders);
        notificationEmailSender.send(request.getRecipientEmail(), subject, templateLink, request.getTemplateModel());
    }

    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.INTERNAL || channel == NotificationChannel.EMAIL;
    }
}

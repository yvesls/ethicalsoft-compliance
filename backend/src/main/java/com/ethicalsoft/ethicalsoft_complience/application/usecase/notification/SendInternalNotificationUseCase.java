package com.ethicalsoft.ethicalsoft_complience.application.usecase.notification;

import com.ethicalsoft.ethicalsoft_complience.application.port.notification.InternalNotificationPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTemplatePort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendInternalNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.Notification;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationStatus;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.ResolvePlaceholderPolicy;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SendInternalNotificationUseCase {

    private final NotificationTemplatePort notificationTemplatePort;
    private final InternalNotificationPort internalNotificationPort;
    private final ResolvePlaceholderPolicy resolvePlaceholderPolicy;

    @Transactional
    public Notification execute(SendInternalNotificationCommand command) {
        var template = notificationTemplatePort.findByKey(command.templateKey())
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + command.templateKey()));

        validateSenderRole(command, template);

        var title = resolvePlaceholderPolicy.resolve(template.title(), command.context());
        var body = resolvePlaceholderPolicy.resolve(template.body(), command.context());

        Notification notification = new Notification(
                null,
                command.sender(),
                command.recipient(),
                title,
                body,
                NotificationStatus.UNREAD,
                LocalDateTime.now(),
                null,
                template.key()
        );

        try {
            return internalNotificationPort.save(notification);
        } catch (Exception ex) {
            throw new BusinessException("Erro no envio de notificação.", ex);
        }
    }

    private void validateSenderRole(SendInternalNotificationCommand cmd, com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate template) {
        if (template.whoCanSend() == null || template.whoCanSend().isEmpty()) {
            return;
        }

        boolean allowed = cmd.sender() != null && cmd.sender().roles() != null
                && cmd.sender().roles().stream().anyMatch(r -> template.whoCanSend().contains(r));
        if (!allowed) {
            throw new AccessDeniedException("Usuário não possui permissão para enviar esta notificação.");
        }
    }
}


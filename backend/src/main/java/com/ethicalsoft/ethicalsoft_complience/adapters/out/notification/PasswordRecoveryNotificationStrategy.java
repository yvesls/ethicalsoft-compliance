package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordRecoveryNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.PASSWORD_RECOVERY;
    }

    @Override
    public void send(SendNotificationCommand command) {
        List<String> recipients = sendSupport.resolveRecipients(command.context(), null, null, false);
        if (recipients.isEmpty()) {
            return;
        }

        String code = Optional.ofNullable(command.context().get("code")).map(Object::toString).orElse("");
        NotificationTemplate template = sendSupport.loadTemplate(type());

        var currentUser = safeCurrentUser();
        sendSupport.validateCanSend(template, currentUser);
        Long projectId = (Long) command.context().get("projectId");
        NotificationSendSupport.SenderData sender = sendSupport.buildSender(currentUser, projectId);

        Map<String, String> placeholders = Map.of("code", code, "recipient", "");

        recipients.forEach(to -> {
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, null);
            channelSender.send(template, placeholders, builder -> sendSupport.applyParticipants(builder, sender, recipient, new java.util.HashMap<>(placeholders)));
        });
    }

    private com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User safeCurrentUser() {
        try {
            return sendSupport.currentUserPort().getCurrentUser();
        } catch (Exception ignored) {
            return null;
        }
    }
}

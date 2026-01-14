package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NewUserCredentialsNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.NEW_USER_CREDENTIALS;
    }

    @Override
    public void send(SendNotificationCommand command) {
        var template = sendSupport.loadTemplate(type());
        sendSupport.validateCanSend(template);

        Long projectId = (Long) command.context().get("projectId");
        List<String> recipients = sendSupport.resolveRecipients(command.context(), template, projectId, false);
        if (recipients.isEmpty()) {
            return;
        }

        String firstName = Optional.ofNullable(command.context().get("firstName")).map(Object::toString).orElse("");
        String tempPassword = Optional.ofNullable(command.context().get("tempPassword")).map(Object::toString).orElse("");
        String projectName = Optional.ofNullable(command.context().get("projectName")).map(Object::toString).orElse("");
        String adminName = Optional.ofNullable(command.context().get("adminName")).map(Object::toString).orElse("");

        Map<String, String> commonPlaceholders = Map.of(
                "firstName", firstName,
                "tempPassword", tempPassword,
                "projectName", projectName,
                "adminName", adminName
        );

        NotificationSendSupport.SenderData sender = sendSupport.buildSender(projectId);

        recipients.forEach(to -> {
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, firstName);
            channelSender.send(template, commonPlaceholders, builder -> {
                sendSupport.applyParticipants(builder, sender, recipient, new HashMap<>(commonPlaceholders));
            });
        });
    }
}

package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepresentativeEmailChangedNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.REPRESENTATIVE_EMAIL_CHANGED;
    }

    @Override
    public void send(SendNotificationCommand command) {
        try {
            NotificationTemplate template = sendSupport.loadTemplate(type());
            sendSupport.validateCanSend(template);

            String to = Optional.ofNullable(command.context().get("to")).map(Object::toString).orElse("");
            String oldEmail = Optional.ofNullable(command.context().get("oldEmail")).map(Object::toString).orElse("");
            String newEmail = Optional.ofNullable(command.context().get("newEmail")).map(Object::toString).orElse("");
            String projectName = Optional.ofNullable(command.context().get("projectName")).map(Object::toString).orElse("");

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("oldEmail", oldEmail);
            placeholders.put("newEmail", newEmail);
            placeholders.put("projectName", projectName);

            channelSender.send(template, placeholders, builder ->
                    builder.recipientEmail(to)
            );

            log.info("[notification] REPRESENTATIVE_EMAIL_CHANGED enviada para {}", to);
        } catch (Exception e) {
            log.warn("[notification] Falha ao enviar REPRESENTATIVE_EMAIL_CHANGED: {}", e.getMessage());
        }
    }
}


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
public class ProjectUnassignmentNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.PROJECT_UNASSIGNMENT;
    }

    @Override
    public void send(SendNotificationCommand command) {
        try {
            NotificationTemplate template = sendSupport.loadTemplate(type());
            sendSupport.validateCanSend(template);

            String to = Optional.ofNullable(command.context().get("to")).map(Object::toString).orElse("");
            String firstName = Optional.ofNullable(command.context().get("firstName")).map(Object::toString).orElse("");
            String projectName = Optional.ofNullable(command.context().get("projectName")).map(Object::toString).orElse("");
            String adminName = Optional.ofNullable(command.context().get("adminName")).map(Object::toString).orElse("");
            String adminEmail = Optional.ofNullable(command.context().get("adminEmail")).map(Object::toString).orElse("");
            Long userId = command.context().get("userId") != null
                    ? Long.valueOf(command.context().get("userId").toString()) : null;

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("firstName", firstName);
            placeholders.put("projectName", projectName);
            placeholders.put("adminName", adminName);

            channelSender.send(template, placeholders, builder ->
                    builder.recipientEmail(to)
                            .recipientUserId(userId)
                            .recipientName(firstName)
                            .senderName(adminName)
                            .senderEmail(adminEmail)
            );

            log.info("[notification] PROJECT_UNASSIGNMENT enviada para {}", to);
        } catch (Exception e) {
            log.warn("[notification] Falha ao enviar PROJECT_UNASSIGNMENT: {}", e.getMessage());
        }
    }
}


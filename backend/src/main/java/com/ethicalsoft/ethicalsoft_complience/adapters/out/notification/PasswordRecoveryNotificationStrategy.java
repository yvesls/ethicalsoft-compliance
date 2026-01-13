package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTemplatePort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTypeStrategy;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationAuthorizationPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.ResolvePlaceholderPolicy;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordRecoveryNotificationStrategy implements NotificationTypeStrategy {

    private final NotificationTemplatePort notificationTemplatePort;
    private final ChannelSender channelSender;
    private final CurrentUserPort currentUserPort;
    private final NotificationRoleResolver notificationRoleResolver;
    private final ResolvePlaceholderPolicy resolvePlaceholderPolicy;
    private final NotificationAuthorizationPolicy authorizationPolicy = new NotificationAuthorizationPolicy();

    @Override
    public NotificationType type() {
        return NotificationType.PASSWORD_RECOVERY;
    }

    @Override
    public void send(SendNotificationCommand command) {
        List<String> recipients = resolveRecipients(command, null);
        if (recipients.isEmpty()) {
            return;
        }

        String code = Optional.ofNullable(command.context().get("code")).map(Object::toString).orElse("");

        var template = notificationTemplatePort.findByKey(type().templateKey())
                .orElseThrow(() -> new EntityNotFoundException("Template não encontrado: " + type().templateKey()));

        var currentUser = safeCurrentUser();

        if (currentUser != null) {
            authorizationPolicy.validateCanSend(template.whoCanSend(), currentUser.getRole(), List.of(currentUser.getRole().name()));
        } else {
            authorizationPolicy.validateCanSend(template.whoCanSend(), null, List.of());
        }

        Long projectId = (Long) command.context().get("projectId");
        List<String> senderRoles = currentUser != null ? notificationRoleResolver.resolveRoles(currentUser.getEmail(), projectId) : List.of();

        Map<String, String> placeholders = Map.of("code", code, "recipient", "");

        recipients.forEach(to -> {
            List<String> recipientRoles = notificationRoleResolver.resolveRoles(to, projectId);
            channelSender.send(template, placeholders, builder -> builder
                    .senderUserId(currentUser != null ? currentUser.getId() : null)
                    .senderName(currentUser != null ? (currentUser.getFirstName() + " " + currentUser.getLastName()) : null)
                    .senderEmail(currentUser != null ? currentUser.getEmail() : null)
                    .senderRoles(senderRoles)
                    .recipientEmail(to)
                    .recipientName(null)
                    .recipientRoles(recipientRoles)
                    .templateModel(new java.util.HashMap<>(placeholders))
            );
        });
    }

    private List<String> resolveRecipients(SendNotificationCommand command, NotificationTemplate template) {
        Object provided = command.context().get("recipients");
        List<String> recipients = new ArrayList<>();
        if (provided instanceof List<?> list) {
            list.stream().map(Object::toString).forEach(recipients::add);
        } else if (command.context().get("to") != null) {
            recipients.add(command.context().get("to").toString());
        } else if (template != null && template.recipients() != null) {
            recipients.addAll(template.recipients());
        }
        return recipients;
    }

    private com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User safeCurrentUser() {
        try {
            return currentUserPort.getCurrentUser();
        } catch (Exception ignored) {
            return null;
        }
    }
}

package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTemplatePort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTypeStrategy;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationAuthorizationPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class NewUserCredentialsNotificationStrategy implements NotificationTypeStrategy {

    private final NotificationTemplatePort notificationTemplatePort;
    private final ChannelSender channelSender;
    private final CurrentUserPort currentUserPort;
    private final NotificationRoleResolver notificationRoleResolver;
    private final NotificationAuthorizationPolicy authorizationPolicy = new NotificationAuthorizationPolicy();

    @Override
    public NotificationType type() {
        return NotificationType.NEW_USER_CREDENTIALS;
    }

    @Override
    public void send(SendNotificationCommand command) {
        var template = notificationTemplatePort.findByKey(type().templateKey())
                .orElseThrow(() -> new EntityNotFoundException("Template não encontrado: " + type().templateKey()));

        var currentUser = currentUserPort.getCurrentUser();
        authorizationPolicy.validateCanSend(template.whoCanSend(),
                currentUser != null ? currentUser.getRole() : null,
                currentUser != null && currentUser.getRole() != null ? List.of(currentUser.getRole().name()) : List.of());

        Long projectId = (Long) command.context().get("projectId");
        List<String> recipients = resolveRecipients(command, template);
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

        List<String> senderRoles = currentUser != null ? notificationRoleResolver.resolveRoles(currentUser.getEmail(), projectId) : List.of();

        recipients.forEach(to -> {
            List<String> recipientRoles = notificationRoleResolver.resolveRoles(to, projectId);
            channelSender.send(template, commonPlaceholders, builder -> builder
                    .senderUserId(currentUser != null ? currentUser.getId() : null)
                    .senderName(currentUser != null ? (currentUser.getFirstName() + " " + currentUser.getLastName()) : null)
                    .senderEmail(currentUser != null ? currentUser.getEmail() : null)
                    .senderRoles(senderRoles)
                    .recipientEmail(to)
                    .recipientName(firstName)
                    .recipientRoles(recipientRoles)
                    .templateModel(new HashMap<>(commonPlaceholders))
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
}

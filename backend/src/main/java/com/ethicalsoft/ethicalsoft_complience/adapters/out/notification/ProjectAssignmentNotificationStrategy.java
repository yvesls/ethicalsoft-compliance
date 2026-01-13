package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.UserRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectAssignmentNotificationStrategy implements NotificationTypeStrategy {

    private final NotificationTemplatePort notificationTemplatePort;
    private final ChannelSender channelSender;
    private final CurrentUserPort currentUserPort;
    private final NotificationRoleResolver notificationRoleResolver;
    private final UserRepository userRepository;
    private final NotificationAuthorizationPolicy authorizationPolicy = new NotificationAuthorizationPolicy();

    @Override
    public NotificationType type() {
        return NotificationType.PROJECT_ASSIGNMENT;
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

        String firstName = Optional.ofNullable(command.context().get("firstName")).map(Object::toString).orElse("");
        String projectName = Optional.ofNullable(command.context().get("projectName")).map(Object::toString).orElse("");
        String adminName = Optional.ofNullable(command.context().get("adminName")).map(Object::toString).orElse("");
        String adminEmail = Optional.ofNullable(command.context().get("adminEmail")).map(Object::toString).orElse("");
        String timelineSummary = Optional.ofNullable(command.context().get("timelineSummary")).map(Object::toString).orElse("");
        LocalDate startDate = (LocalDate) command.context().get("startDate");
        LocalDate deadline = (LocalDate) command.context().get("deadline");
        LocalDate nextQuestionnaireDate = (LocalDate) command.context().get("nextQuestionnaireDate");
        Object rolesObj = command.context().get("roles");
        List<String> rolesList = rolesObj instanceof List<?> list ? list.stream().map(Object::toString).toList() : List.of();
        String roles = String.join(", ", rolesList);
        String projectLink = Optional.ofNullable(command.context().get("projectLink"))
                .map(Object::toString)
                .orElseGet(() -> projectId != null ? "/projects/" + projectId : "");
        String environment = Optional.ofNullable(command.context().get("environment"))
                .map(Object::toString)
                .orElse("");
        String supportEmail = Optional.ofNullable(command.context().get("supportEmail"))
                .map(Object::toString)
                .orElse("");

        Map<String, String> placeholders = java.util.Map.ofEntries(
                java.util.Map.entry("firstName", firstName),
                java.util.Map.entry("projectName", projectName),
                java.util.Map.entry("adminName", adminName),
                java.util.Map.entry("adminEmail", adminEmail),
                java.util.Map.entry("roles", roles),
                java.util.Map.entry("timelineSummary", timelineSummary),
                java.util.Map.entry("startDateFormatted", startDate != null ? startDate.toString() : ""),
                java.util.Map.entry("deadlineFormatted", deadline != null ? deadline.toString() : ""),
                java.util.Map.entry("nextQuestionnaireFormatted", nextQuestionnaireDate != null ? nextQuestionnaireDate.toString() : ""),
                java.util.Map.entry("projectLink", projectLink),
                java.util.Map.entry("environment", environment),
                java.util.Map.entry("supportEmail", supportEmail)
        );

        Map<String, Object> model = new java.util.HashMap<>(placeholders);
        model.put("roles", rolesList);

        List<String> recipients = resolveRecipients(command, template);
        List<String> senderRoles = currentUser != null ? notificationRoleResolver.resolveRoles(currentUser.getEmail(), projectId) : List.of();

        recipients.forEach(to -> {
            List<String> recipientRoles = notificationRoleResolver.resolveRoles(to, projectId);
            Long recipientUserId = userRepository.findByEmail(to).map(User::getId).orElse(null);
            channelSender.send(template, placeholders, builder -> builder
                    .senderUserId(currentUser != null ? currentUser.getId() : null)
                    .senderName(currentUser != null ? (currentUser.getFirstName() + " " + currentUser.getLastName()) : null)
                    .senderEmail(currentUser != null ? currentUser.getEmail() : null)
                    .senderRoles(senderRoles)
                    .recipientUserId(recipientUserId)
                    .recipientEmail(to)
                    .recipientName(firstName)
                    .recipientRoles(recipientRoles)
                    .templateModel(model)
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

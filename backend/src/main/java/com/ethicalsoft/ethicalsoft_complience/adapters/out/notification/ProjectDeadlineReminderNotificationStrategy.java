package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
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

import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ProjectDeadlineReminderNotificationStrategy implements NotificationTypeStrategy {

    private final NotificationTemplatePort notificationTemplatePort;
    private final ChannelSender channelSender;
    private final CurrentUserPort currentUserPort;
    private final NotificationRoleResolver notificationRoleResolver;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationAuthorizationPolicy authorizationPolicy = new NotificationAuthorizationPolicy();

    @Override
    public NotificationType type() {
        return NotificationType.DEADLINE_REMINDER;
    }

    @Override
    public void send(SendNotificationCommand command) {
        NotificationTemplate template = notificationTemplatePort.findByKey(type().templateKey())
                .orElseThrow(() -> new EntityNotFoundException("Template não encontrado: " + type().templateKey()));

        var currentUser = currentUserPort.getCurrentUser();
        authorizationPolicy.validateCanSend(template.whoCanSend(),
                currentUser != null ? currentUser.getRole() : null,
                currentUser != null && currentUser.getRole() != null ? List.of(currentUser.getRole().name()) : List.of());

        Long projectId = (Long) command.context().get("projectId");
        if (projectId == null) return;
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return;

        long daysRemaining = Optional.ofNullable(command.context().get("daysRemaining")).map(Long.class::cast)
                .orElse(0L);
        String projectName = Optional.ofNullable(command.context().get("projectName")).map(Object::toString)
                .orElse(project.getName());
        String deadlineFormatted = Optional.ofNullable(project.getDeadline())
                .map(d -> d.format(DateTimeFormatter.ISO_DATE)).orElse("");
        String environment = Optional.ofNullable(command.context().get("environment")).map(Object::toString).orElse("");
        String projectLink = Optional.ofNullable(command.context().get("projectLink")).map(Object::toString)
                .orElse("/projects/" + projectId);

        Map<String, String> placeholders = Map.ofEntries(
                Map.entry("projectName", projectName),
                Map.entry("deadlineFormatted", deadlineFormatted),
                Map.entry("daysRemaining", String.valueOf(daysRemaining)),
                Map.entry("projectLink", projectLink),
                Map.entry("environment", environment)
        );
        Map<String, Object> model = new HashMap<>(placeholders);

        List<String> recipients = resolveRecipients(command, template, project);
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
                    .recipientName(null)
                    .recipientRoles(recipientRoles)
                    .templateModel(model)
            );
        });
    }

    private List<String> resolveRecipients(SendNotificationCommand command, NotificationTemplate template, Project project) {
        List<String> recipients = new ArrayList<>();
        Object provided = command.context().get("recipients");
        if (provided instanceof List<?> list) {
            list.stream().map(Object::toString).forEach(recipients::add);
        }
        if (!recipients.isEmpty()) return recipients;

        if (project.getOwner() != null && project.getOwner().getEmail() != null) {
            recipients.add(project.getOwner().getEmail());
        }
        project.getRepresentatives().forEach(rep -> {
            if (rep.getUser() != null && rep.getUser().getEmail() != null) {
                recipients.add(rep.getUser().getEmail());
            }
        });

        if (recipients.isEmpty() && template.recipients() != null) {
            recipients.addAll(template.recipients());
        }
        return recipients;
    }
}


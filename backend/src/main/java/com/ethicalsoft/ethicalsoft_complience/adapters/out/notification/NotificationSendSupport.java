package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.UserRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTemplatePort;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationAuthorizationPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class NotificationSendSupport {

    private final NotificationTemplatePort notificationTemplatePort;
    private final CurrentUserPort currentUserPort;
    private final NotificationRoleResolver notificationRoleResolver;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationAuthorizationPolicy authorizationPolicy = new NotificationAuthorizationPolicy();

    public NotificationTemplate loadTemplate(NotificationType type) {
        return notificationTemplatePort.findByKey(type.templateKey())
                .orElseThrow(() -> new EntityNotFoundException("Template não encontrado: " + type.templateKey()));
    }

    public void validateCanSend(NotificationTemplate template) {
        var currentUser = currentUserPort.getCurrentUser();
        authorizationPolicy.validateCanSend(
                template.whoCanSend(),
                currentUser != null ? currentUser.getRole() : null,
                currentUser != null && currentUser.getRole() != null ? List.of(currentUser.getRole().name()) : List.of()
        );
    }

    public void validateCanSend(NotificationTemplate template, User currentUser) {
        authorizationPolicy.validateCanSend(
                template.whoCanSend(),
                currentUser != null ? currentUser.getRole() : null,
                currentUser != null && currentUser.getRole() != null ? List.of(currentUser.getRole().name()) : List.of()
        );
    }

    public SenderData buildSender(Long projectId) {
        var currentUser = currentUserPort.getCurrentUser();
        if (currentUser == null) {
            return new SenderData(null, null, null, List.of());
        }
        String fullName = String.format("%s %s", Optional.ofNullable(currentUser.getFirstName()).orElse(""),
                Optional.ofNullable(currentUser.getLastName()).orElse("")).trim();
        List<String> roles = notificationRoleResolver.resolveRoles(currentUser.getEmail(), projectId);
        return new SenderData(currentUser.getId(), fullName.isBlank() ? null : fullName, currentUser.getEmail(), roles);
    }

    public SenderData buildSender(User currentUser, Long projectId) {
        if (currentUser == null) {
            return new SenderData(null, null, null, List.of());
        }
        String fullName = String.format("%s %s", Optional.ofNullable(currentUser.getFirstName()).orElse(""),
                Optional.ofNullable(currentUser.getLastName()).orElse("")).trim();
        List<String> roles = notificationRoleResolver.resolveRoles(currentUser.getEmail(), projectId);
        return new SenderData(currentUser.getId(), fullName.isBlank() ? null : fullName, currentUser.getEmail(), roles);
    }

    public RecipientData buildRecipient(String email, Long projectId, String nameFallback) {
        if (email == null) {
            return new RecipientData(null, null, null, List.of());
        }
        Long userId = userRepository.findByEmail(email).map(User::getId).orElse(null);
        List<String> roles = notificationRoleResolver.resolveRoles(email, projectId);
        return new RecipientData(userId, nameFallback, email, roles);
    }

    public List<String> resolveRecipients(Map<String, Object> context, NotificationTemplate template, Long projectId, boolean includeProjectOwnerFallback) {
        List<String> recipients = new ArrayList<>();
        Object provided = context.get("recipients");
        if (provided instanceof List<?> list) {
            list.stream().map(Objects::toString).forEach(recipients::add);
        } else if (context.get("to") != null) {
            recipients.add(context.get("to").toString());
        }
        if (!recipients.isEmpty()) {
            return recipients;
        }
        if (includeProjectOwnerFallback && projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null && project.getOwner() != null && project.getOwner().getEmail() != null) {
                recipients.add(project.getOwner().getEmail());
            }
        }
        if (recipients.isEmpty() && template != null && template.recipients() != null) {
            recipients.addAll(template.recipients());
        }
        return recipients;
    }

    public void applyParticipants(NotificationDispatchRequest.NotificationDispatchRequestBuilder builder,
                                  SenderData sender,
                                  RecipientData recipient,
                                  Map<String, Object> templateModel) {
        builder.senderUserId(sender.id())
                .senderName(sender.name())
                .senderEmail(sender.email())
                .senderRoles(sender.roles())
                .recipientUserId(recipient.id())
                .recipientEmail(recipient.email())
                .recipientName(recipient.name())
                .recipientRoles(recipient.roles())
                .templateModel(templateModel != null ? new HashMap<>(templateModel) : new HashMap<>());
    }

    public CurrentUserPort currentUserPort() {
        return currentUserPort;
    }

    public record SenderData(Long id, String name, String email, List<String> roles) {}

    public record RecipientData(Long id, String name, String email, List<String> roles) {}
}

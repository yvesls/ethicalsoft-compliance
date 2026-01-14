package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectAssignmentNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.PROJECT_ASSIGNMENT;
    }

    @Override
    public void send(SendNotificationCommand command) {
        NotificationTemplate template = sendSupport.loadTemplate(type());
        sendSupport.validateCanSend(template);

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

        Map<String, String> placeholders = Map.ofEntries(
                Map.entry("firstName", firstName),
                Map.entry("projectName", projectName),
                Map.entry("adminName", adminName),
                Map.entry("adminEmail", adminEmail),
                Map.entry("roles", roles),
                Map.entry("timelineSummary", timelineSummary),
                Map.entry("startDateFormatted", startDate != null ? startDate.toString() : ""),
                Map.entry("deadlineFormatted", deadline != null ? deadline.toString() : ""),
                Map.entry("nextQuestionnaireFormatted", nextQuestionnaireDate != null ? nextQuestionnaireDate.toString() : ""),
                Map.entry("projectLink", projectLink),
                Map.entry("environment", environment),
                Map.entry("supportEmail", supportEmail)
        );
        Map<String, Object> model = new HashMap<>(placeholders);
        model.put("roles", rolesList);

        List<String> recipients = sendSupport.resolveRecipients(command.context(), template, projectId, false);
        if (recipients.isEmpty()) {
            return;
        }

        NotificationSendSupport.SenderData sender = sendSupport.buildSender(projectId);

        recipients.forEach(to -> {
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, firstName);
            channelSender.send(template, placeholders, builder -> sendSupport.applyParticipants(builder, sender, recipient, model));
        });
    }
}

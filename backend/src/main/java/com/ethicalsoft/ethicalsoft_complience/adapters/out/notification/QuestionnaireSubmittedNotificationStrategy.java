package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuestionnaireSubmittedNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.QUESTIONNAIRE_SUBMITTED;
    }

    @Override
    public void send(SendNotificationCommand command) {
        NotificationTemplate template = sendSupport.loadTemplate(type());
        sendSupport.validateCanSend(template);

        Long projectId = (Long) command.context().get("projectId");
        String questionnaireName = Optional.ofNullable(command.context().get("questionnaireName")).map(Object::toString).orElse("");
        String projectName = Optional.ofNullable(command.context().get("projectName")).map(Object::toString).orElse("");
        String responderEmail = Optional.ofNullable(command.context().get("responderEmail")).map(Object::toString).orElse("");
        String responderName = Optional.ofNullable(command.context().get("responderName")).map(Object::toString).orElse("");
        LocalDateTime submittedAt = (LocalDateTime) command.context().get("submittedAt");
        String submittedAtFormatted = submittedAt != null ? submittedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
        String environment = Optional.ofNullable(command.context().get("environment")).map(Object::toString).orElse("");
        String projectLink = Optional.ofNullable(command.context().get("projectLink")).map(Object::toString)
                .orElseGet(() -> projectId != null ? "/projects/" + projectId : "");

        Map<String, String> placeholders = Map.ofEntries(
                Map.entry("questionnaireName", questionnaireName),
                Map.entry("projectName", projectName),
                Map.entry("responderName", responderName),
                Map.entry("responderEmail", responderEmail),
                Map.entry("submittedAtFormatted", submittedAtFormatted),
                Map.entry("environment", environment),
                Map.entry("projectLink", projectLink)
        );

        Map<String, Object> model = new java.util.HashMap<>(placeholders);

        List<String> recipients = sendSupport.resolveRecipients(command.context(), template, projectId, true);
        NotificationSendSupport.SenderData sender = sendSupport.buildSender(projectId);

        recipients.forEach(to -> {
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, null);
            channelSender.send(template, placeholders, builder -> sendSupport.applyParticipants(builder, sender, recipient, model));
        });
    }
}

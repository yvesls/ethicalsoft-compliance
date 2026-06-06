package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectOverdueNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.PROJECT_OVERDUE;
    }

    @Override
    public void send(SendNotificationCommand command) {
        NotificationTemplate template = sendSupport.loadTemplate(type());

        Long projectId = (Long) command.context().get("projectId");
        String projectName = Optional.ofNullable(command.context().get("projectName"))
                .map(Object::toString).orElse("");
        String deadlineFormatted = Optional.ofNullable(command.context().get("deadline"))
                .map(v -> v instanceof LocalDate d
                        ? d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : v.toString())
                .orElse("");
        String pendingQuestionnaires = Optional.ofNullable(command.context().get("pendingQuestionnaires"))
                .map(Object::toString).orElse("0");
        String totalQuestionnaires = Optional.ofNullable(command.context().get("totalQuestionnaires"))
                .map(Object::toString).orElse("0");
        String environment = Optional.ofNullable(command.context().get("environment"))
                .map(Object::toString).orElse("");
        String projectLink = Optional.ofNullable(command.context().get("projectLink"))
                .map(Object::toString)
                .orElseGet(() -> projectId != null ? "/projects/" + projectId : "");

        List<String> recipients = sendSupport.resolveRecipients(command.context(), template, projectId, true);

        if (recipients.isEmpty()) {
            log.warn("[notification-project-overdue] Nenhum destinatário encontrado para projeto atrasado. projeto={}", projectId);
            return;
        }

        NotificationSendSupport.SenderData sender = sendSupport.buildSender(projectId);

        recipients.forEach(to -> {
            Map<String, String> placeholders = Map.ofEntries(
                    Map.entry("recipientName", to),
                    Map.entry("projectName", projectName),
                    Map.entry("deadlineFormatted", deadlineFormatted),
                    Map.entry("pendingQuestionnaires", pendingQuestionnaires),
                    Map.entry("totalQuestionnaires", totalQuestionnaires),
                    Map.entry("environment", environment),
                    Map.entry("projectLink", projectLink)
            );
            Map<String, Object> model = new HashMap<>(placeholders);
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, null);
            channelSender.send(template, placeholders, builder ->
                    sendSupport.applyParticipants(builder, sender, recipient, model));
        });

        log.info("[notification-project-overdue] Notificação de projeto ATRASADO enviada. projeto={} destinatários={}",
                projectId, recipients.size());
    }
}


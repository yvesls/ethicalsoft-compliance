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
public class QuestionnaireOverdueNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.QUESTIONNAIRE_OVERDUE;
    }

    @Override
    public void send(SendNotificationCommand command) {
        NotificationTemplate template = sendSupport.loadTemplate(type());

        Long projectId = (Long) command.context().get("projectId");
        String questionnaireName = Optional.ofNullable(command.context().get("questionnaireName"))
                .map(Object::toString).orElse("");
        String projectName = Optional.ofNullable(command.context().get("projectName"))
                .map(Object::toString).orElse("");
        String expiredAtFormatted = Optional.ofNullable(command.context().get("expiredAt"))
                .map(v -> v instanceof LocalDate d
                        ? d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : v.toString())
                .orElse("");
        String pendingCount = Optional.ofNullable(command.context().get("pendingCount"))
                .map(Object::toString).orElse("0");
        String totalCount = Optional.ofNullable(command.context().get("totalCount"))
                .map(Object::toString).orElse("0");
        String environment = Optional.ofNullable(command.context().get("environment"))
                .map(Object::toString).orElse("");
        String projectLink = Optional.ofNullable(command.context().get("projectLink"))
                .map(Object::toString)
                .orElseGet(() -> projectId != null ? "/projects/" + projectId : "");

        List<String> recipients = sendSupport.resolveRecipients(command.context(), template, projectId, true);

        if (recipients.isEmpty()) {
            log.warn("[notification-overdue] Nenhum destinatário encontrado para questionário atrasado. projeto={}", projectId);
            return;
        }

        NotificationSendSupport.SenderData sender = sendSupport.buildSender(projectId);

        recipients.forEach(to -> {
            Map<String, String> placeholders = Map.ofEntries(
                    Map.entry("recipientName", to),
                    Map.entry("questionnaireName", questionnaireName),
                    Map.entry("projectName", projectName),
                    Map.entry("expiredAtFormatted", expiredAtFormatted),
                    Map.entry("pendingCount", pendingCount),
                    Map.entry("totalCount", totalCount),
                    Map.entry("environment", environment),
                    Map.entry("projectLink", projectLink)
            );
            Map<String, Object> model = new HashMap<>(placeholders);
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, null);
            channelSender.send(template, placeholders, builder ->
                    sendSupport.applyParticipants(builder, sender, recipient, model));
        });

        log.info("[notification-overdue] Notificação de questionário ATRASADO enviada. questionário={} projeto={} destinatários={}",
                questionnaireName, projectId, recipients.size());
    }
}


package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectDeadlineExceededWarningNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.PROJECT_DEADLINE_EXCEEDED_WARNING;
    }

    @Override
    public void send(SendNotificationCommand command) {
        NotificationTemplate template = sendSupport.loadTemplate(type());

        Long projectId = (Long) command.context().get("projectId");
        String projectName = str(command, "projectName");
        String questionnaireName = str(command, "questionnaireName");
        String newEndDate = str(command, "newEndDate");
        String deadline = str(command, "deadline");
        String warning = str(command, "warning");
        String projectLink = Optional.ofNullable(command.context().get("projectLink"))
                .map(Object::toString)
                .orElseGet(() -> projectId != null ? "/projects/" + projectId : "");

        List<String> recipients = sendSupport.resolveRecipients(command.context(), template, projectId, true);

        if (recipients.isEmpty()) {
            log.warn("[notification-deadline-exceeded] Nenhum destinatário encontrado. projeto={}", projectId);
            return;
        }

        NotificationSendSupport.SenderData sender = sendSupport.buildSender(projectId);

        recipients.forEach(to -> {
            Map<String, String> placeholders = Map.ofEntries(
                    Map.entry("recipientName", to),
                    Map.entry("projectName", projectName),
                    Map.entry("questionnaireName", questionnaireName),
                    Map.entry("newEndDate", newEndDate),
                    Map.entry("deadline", deadline),
                    Map.entry("warning", warning),
                    Map.entry("projectLink", projectLink)
            );
            Map<String, Object> model = new HashMap<>(placeholders);
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, null);
            channelSender.send(template, placeholders, builder ->
                    sendSupport.applyParticipants(builder, sender, recipient, model));
        });

        log.info("[notification-deadline-exceeded] Notificação de prazo excedido enviada. projeto={} destinatários={}",
                projectId, recipients.size());
    }

    private String str(SendNotificationCommand cmd, String key) {
        return Optional.ofNullable(cmd.context().get(key)).map(Object::toString).orElse("");
    }
}


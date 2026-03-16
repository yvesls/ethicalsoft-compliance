package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectIsepCalculatedNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.PROJECT_ISEP_CALCULATED;
    }

    @Override
    public void send(SendNotificationCommand command) {
        NotificationTemplate template = sendSupport.loadTemplate(type());

        Long projectId = (Long) command.context().get("projectId");
        String projectName = Optional.ofNullable(command.context().get("projectName"))
                .map(Object::toString).orElse("");
        String isepPercent = Optional.ofNullable(command.context().get("isepPercent"))
                .map(Object::toString).orElse("");
        String band = Optional.ofNullable(command.context().get("band"))
                .map(Object::toString).orElse("");
        String closedBy = Optional.ofNullable(command.context().get("closedBy"))
                .map(Object::toString).orElse("Sistema");
        String totalQuestionnaires = Optional.ofNullable(command.context().get("totalQuestionnaires"))
                .map(Object::toString).orElse("0");
        String calculatedAtFormatted = Optional.ofNullable(command.context().get("calculatedAt"))
                .map(v -> v instanceof LocalDateTime dt
                        ? dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : v.toString())
                .orElse(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        String environment = Optional.ofNullable(command.context().get("environment"))
                .map(Object::toString).orElse("");
        String projectLink = Optional.ofNullable(command.context().get("projectLink"))
                .map(Object::toString)
                .orElseGet(() -> projectId != null ? "/projects/" + projectId : "");

        List<String> recipients = sendSupport.resolveRecipients(command.context(), template, projectId, true);

        if (recipients.isEmpty()) {
            log.warn("[notification-project-isep] Nenhum destinatário encontrado para ISEP de projeto. projeto={}", projectId);
            return;
        }

        NotificationSendSupport.SenderData sender = sendSupport.buildSender(projectId);

        recipients.forEach(to -> {
            Map<String, String> placeholders = Map.ofEntries(
                    Map.entry("recipientName", to),
                    Map.entry("projectName", projectName),
                    Map.entry("isepPercent", isepPercent),
                    Map.entry("band", band),
                    Map.entry("closedBy", closedBy),
                    Map.entry("totalQuestionnaires", totalQuestionnaires),
                    Map.entry("calculatedAtFormatted", calculatedAtFormatted),
                    Map.entry("environment", environment),
                    Map.entry("projectLink", projectLink)
            );
            Map<String, Object> model = new HashMap<>(placeholders);
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, null);
            channelSender.send(template, placeholders, builder ->
                    sendSupport.applyParticipants(builder, sender, recipient, model));
        });

        log.info("[notification-project-isep] Notificação de ISEP consolidado do projeto enviada. ISEP={}% faixa={} projeto={} destinatários={}",
                isepPercent, band, projectId, recipients.size());
    }
}


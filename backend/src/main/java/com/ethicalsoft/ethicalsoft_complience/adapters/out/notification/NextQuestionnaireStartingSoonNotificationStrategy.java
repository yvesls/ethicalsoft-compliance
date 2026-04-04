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
public class NextQuestionnaireStartingSoonNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Override
    public NotificationType type() {
        return NotificationType.NEXT_QUESTIONNAIRE_STARTING_SOON;
    }

    @Override
    public void send(SendNotificationCommand command) {
        NotificationTemplate template = sendSupport.loadTemplate(type());

        Long projectId = (Long) command.context().get("projectId");
        String projectName = str(command, "projectName");
        String nextQuestionnaireName = str(command, "nextQuestionnaireName");
        String nextStartDate = str(command, "nextStartDate");
        String nextEndDate = str(command, "nextEndDate");
        String closedQuestionnaireName = str(command, "closedQuestionnaireName");
        String daysUntilStart = str(command, "daysUntilStart");
        String projectLink = Optional.ofNullable(command.context().get("projectLink"))
                .map(Object::toString)
                .orElseGet(() -> projectId != null ? "/projects/" + projectId : "");

        List<String> recipients = sendSupport.resolveRecipients(command.context(), template, projectId, true);

        if (recipients.isEmpty()) {
            log.warn("[notification-next-questionnaire] Nenhum destinatário encontrado. projeto={}", projectId);
            return;
        }

        NotificationSendSupport.SenderData sender = sendSupport.buildSender(projectId);

        recipients.forEach(to -> {
            Map<String, String> placeholders = Map.ofEntries(
                    Map.entry("recipientName", to),
                    Map.entry("projectName", projectName),
                    Map.entry("nextQuestionnaireName", nextQuestionnaireName),
                    Map.entry("nextStartDate", nextStartDate),
                    Map.entry("nextEndDate", nextEndDate),
                    Map.entry("closedQuestionnaireName", closedQuestionnaireName),
                    Map.entry("daysUntilStart", daysUntilStart),
                    Map.entry("projectLink", projectLink)
            );
            Map<String, Object> model = new HashMap<>(placeholders);
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, null);
            channelSender.send(template, placeholders, builder ->
                    sendSupport.applyParticipants(builder, sender, recipient, model));
        });

        log.info("[notification-next-questionnaire] Notificação de próximo questionário enviada. próximo={} projeto={} destinatários={}",
                nextQuestionnaireName, projectId, recipients.size());
    }

    private String str(SendNotificationCommand cmd, String key) {
        return Optional.ofNullable(cmd.context().get(key)).map(Object::toString).orElse("");
    }
}


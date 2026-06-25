package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewUserCredentialsNotificationStrategy implements NotificationTypeStrategy {

    private final ChannelSender channelSender;
    private final NotificationSendSupport sendSupport;

    @Value("${app.frontend.url:}")
    private String frontendBaseUrl;

    @Value("${app.support.email:}")
    private String defaultSupportEmail;

    @Value("${app.environment:}")
    private String defaultEnvironment;

    @Override
    public NotificationType type() {
        return NotificationType.NEW_USER_CREDENTIALS;
    }

    @Override
    public void send(SendNotificationCommand command) {
        var template = sendSupport.loadTemplate(type());
        boolean systemTriggered = Boolean.TRUE.equals(command.context().get("systemTriggered"));
        if (!systemTriggered) {
            sendSupport.validateCanSend(template);
        }

        Long projectId = (Long) command.context().get("projectId");
        List<String> recipients = sendSupport.resolveRecipients(command.context(), template, projectId, false);
        if (recipients.isEmpty()) {
            log.warn("[notification-new-credentials] Nenhum destinatário resolvido para projectId={}", projectId);
            return;
        }

        String firstName = Optional.ofNullable(command.context().get("firstName")).map(value -> value.toString()).orElse("");
        String tempPassword = Optional.ofNullable(command.context().get("tempPassword")).map(value -> value.toString()).orElse("");
        String projectName = Optional.ofNullable(command.context().get("projectName")).map(value -> value.toString()).orElse("");
        String adminName = Optional.ofNullable(command.context().get("adminName")).map(value -> value.toString()).orElse("");
        String resetLink = Optional.ofNullable(command.context().get("resetLink")).map(value -> value.toString())
                .filter(value -> !value.isBlank())
            .orElseGet(this::buildDefaultResetLink);
        String supportEmail = Optional.ofNullable(command.context().get("supportEmail")).map(value -> value.toString())
            .filter(value -> !value.isBlank())
            .orElse(defaultSupportEmail != null ? defaultSupportEmail : "");
        String environment = Optional.ofNullable(command.context().get("environment")).map(value -> value.toString())
            .filter(value -> !value.isBlank())
            .orElse(defaultEnvironment != null ? defaultEnvironment : "");

        Map<String, String> commonPlaceholders = new HashMap<>();
        commonPlaceholders.put("firstName", firstName);
        commonPlaceholders.put("tempPassword", tempPassword);
        commonPlaceholders.put("temporaryPassword", tempPassword);
        commonPlaceholders.put("projectName", projectName);
        commonPlaceholders.put("adminName", adminName);
        commonPlaceholders.put("resetLink", resetLink);
        commonPlaceholders.put("supportEmail", supportEmail);
        commonPlaceholders.put("environment", environment);

        NotificationSendSupport.SenderData sender = sendSupport.buildSender(projectId);

        recipients.forEach(to -> {
            NotificationSendSupport.RecipientData recipient = sendSupport.buildRecipient(to, projectId, firstName);
            log.info("[notification-new-credentials] Enviando credenciais iniciais para={} projectId={} template={}",
                    to, projectId, template.templateLink());
            channelSender.send(template, commonPlaceholders, builder -> {
                sendSupport.applyParticipants(builder, sender, recipient, new HashMap<>(commonPlaceholders));
            });
        });
    }

    private String buildDefaultResetLink() {
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            return "/login";
        }
        String trimmed = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        return trimmed + "/login";
    }
}

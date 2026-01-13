package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.UserRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTemplatePort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTypeStrategy;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationAuthorizationPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationParty;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.criteria.QuestionnaireReminderContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireReminderNotificationStrategy implements NotificationTypeStrategy {

    private final NotificationTemplatePort notificationTemplatePort;
    private final ChannelSender channelSender;
    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final UserRepository userRepository;
    private final CurrentUserPort currentUserPort;
    private final NotificationAuthorizationPolicy authorizationPolicy = new NotificationAuthorizationPolicy();
    private final NotificationRoleResolver notificationRoleResolver;

    @Override
    public NotificationType type() {
        return NotificationType.QUESTIONNAIRE_REMINDER;
    }

    @Override
    public void send(SendNotificationCommand command) {
        Long projectId = (Long) command.context().get("projectId");
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

        var template = notificationTemplatePort.findByKey(type().templateKey())
                .orElseThrow(() -> new EntityNotFoundException("Template não encontrado: " + type().templateKey()));

        var currentUser = currentUserPort.getCurrentUser();
        if (currentUser == null) {
            log.warn("[notification] Usuário atual não encontrado para enviar lembrete interno projeto={}", projectId);
            return;
        }

        authorizationPolicy.validateCanSend(template.whoCanSend(), currentUser.getRole(), List.of(currentUser.getRole().name()));

        var senderRoles = notificationRoleResolver.resolveRoles(currentUser.getEmail(), projectId);
        var sender = new NotificationParty(
                currentUser.getId(),
                currentUser.getFirstName() + " " + currentUser.getLastName(),
                currentUser.getEmail(),
                senderRoles
        );

        Integer questionnaireId = null;
        Object qIdObj = command.context().get("questionnaireId");
        if (qIdObj instanceof Number num) {
            questionnaireId = num.intValue();
        } else if (qIdObj != null) {
            try {
                questionnaireId = Integer.valueOf(qIdObj.toString());
            } catch (NumberFormatException ignored) {
                log.error("[notification] Id do questionário inválido para lembrete interno projeto={} valor={}", projectId, qIdObj);
            }
        }

        Questionnaire questionnaire = null;
        if (questionnaireId != null) {
            questionnaire = questionnaireRepository.findById(questionnaireId).orElse(null);
        }
        if (questionnaire == null) {
            log.warn("[notification] Questionário não encontrado ou id ausente para projeto={}", projectId);
            return;
        }

        if (!TimelineStatusEnum.EM_ANDAMENTO.equals(questionnaire.getStatus())) {
            log.warn("[notification] Questionário não está em andamento para lembrete interno questionário={} projeto={}", questionnaire.getId(), projectId);
            return;
        }

        sendForQuestionnaire(projectId, project.getName(), questionnaire, template, sender, command);
    }

    private void sendForQuestionnaire(Long projectId, String projectName, Questionnaire questionnaire, NotificationTemplate template, NotificationParty sender, SendNotificationCommand command) {
        List<String> providedRecipients = resolveRecipientsFromContext(command, template);
        Set<String> emails = providedRecipients.isEmpty()
                ? resolveEmailsForReminder(projectId, questionnaire.getId())
                : new java.util.HashSet<>(providedRecipients);
        if (emails.isEmpty()) {
            log.info("[notification] Nenhum destinatário para lembrete interno questionário={} projeto={}", questionnaire.getId(), projectId);
            return;
        }

        QuestionnaireReminderContext context = QuestionnaireReminderContext.from(questionnaire);
        for (String email : emails) {
            Long recipientUserId = userRepository.findByEmail(email).map(User::getId).orElse(null);
            List<String> recipientRoles = notificationRoleResolver.resolveRoles(email, projectId);

            Map<String, String> placeholders = Map.of(
                    "recipientName", Optional.ofNullable(email).orElse(""),
                    "senderRole", String.join(",", Optional.ofNullable(sender.roles()).orElse(List.of())),
                    "questionnaireName", Optional.ofNullable(context.questionnaireName()).orElse(""),
                    "projectName", Optional.ofNullable(projectName).orElse(""),
                    "period", Optional.ofNullable(context.period()).orElse("")
            );

            channelSender.send(template, placeholders, builder -> builder
                    .senderUserId(sender.userId())
                    .senderName(sender.fullName())
                    .senderEmail(sender.email())
                    .senderRoles(sender.roles())
                    .recipientUserId(recipientUserId)
                    .recipientEmail(email)
                    .recipientRoles(recipientRoles)
                    .templateModel(new java.util.HashMap<>(placeholders))
            );
        }
    }

    private List<String> resolveRecipientsFromContext(SendNotificationCommand command, NotificationTemplate template) {
        Object provided = command.context().get("recipients");
        if (provided instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        if (template != null && template.recipients() != null) {
            return template.recipients();
        }
        return List.of();
    }

    private Set<String> resolveEmailsForReminder(Long projectId, Integer questionnaireId) {
        List<QuestionnaireResponse> pendingResponses = questionnaireResponseRepository.findPendingResponses(projectId, questionnaireId);
        Set<Long> pendingRepresentativeIds = pendingResponses.stream()
                .map(QuestionnaireResponse::getRepresentativeId)
                .collect(Collectors.toSet());

        return Optional.ofNullable(projectRepository.findById(projectId).orElseThrow().getRepresentatives()).orElse(Set.of())
                .stream()
                .filter(rep -> pendingRepresentativeIds.contains(rep.getId()))
                .map(rep -> rep.getUser().getEmail())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}

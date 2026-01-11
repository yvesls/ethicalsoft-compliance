package com.ethicalsoft.ethicalsoft_complience.adapters.out.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireReminderRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.UserRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.NotificationDispatcherPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireReminderPort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendInternalNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendInternalNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationParty;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.criteria.QuestionnaireReminderContext;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.EmailSendingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireReminderAdapter implements QuestionnaireReminderPort {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final NotificationDispatcherPort notificationDispatcher;
    private final CurrentUserPort currentUserPort;
    private final SendInternalNotificationUseCase sendInternalNotificationUseCase;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public void sendReminder(Long projectId, Integer questionnaireId, QuestionnaireReminderRequestDTO request) {
        try {
            log.info("[questionnaire] Enviando lembrete manual questionário={} projeto={} emails={}", questionnaireId, projectId, request != null ? request.emails() : null);

            projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

            Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                    .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));

            if (!questionnaire.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Questionário não pertence ao projeto informado");
            }

            validateQuestionnaireInProgress(questionnaire);
            Set<String> targetEmails = resolveEmailsForReminder(questionnaire.getProject(), questionnaire.getId(), request);
            QuestionnaireReminderContext context = QuestionnaireReminderContext.from(questionnaire);

            sendReminderEmails(targetEmails, context);
        } catch ( Exception ex ) {
            log.error("[questionnaire] Falha ao enviar lembrete questionário={} projeto={}", questionnaireId, projectId, ex);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void sendAutomaticReminder(Long projectId, Integer questionnaireId) {
        try {
            log.info("[questionnaire] Enviando lembrete automático (por ids) questionário={} projeto={}", questionnaireId, projectId);

            projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

            Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                    .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));

            if (!questionnaire.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Questionário não pertence ao projeto informado");
            }

            if (isQuestionnaireInProgress(questionnaire)) {
                log.info("[questionnaire] Ignorando lembrete automático questionário={} fora da vigência/status", questionnaire.getId());
                return;
            }

            Set<String> emails = resolveEmailsForReminder(questionnaire.getProject(), questionnaire.getId(), null);
            if (emails.isEmpty()) {
                log.info("[questionnaire] Nenhum email pendente para lembrete automático questionário={}", questionnaire.getId());
                return;
            }

            QuestionnaireReminderContext context = QuestionnaireReminderContext.from(questionnaire);
            sendReminderEmails(emails, context);
        } catch (Exception ex) {
            log.error("[questionnaire] Falha ao enviar lembrete automático (por ids) projectId={} questionnaireId={}", projectId, questionnaireId, ex);
            throw ex;
        }
    }

    private void validateQuestionnaireInProgress(Questionnaire questionnaire) {
        if (isQuestionnaireInProgress(questionnaire)) {
            throw new BusinessException("Só é possível enviar lembretes para questionários em andamento.");
        }
    }

    @Transactional(readOnly = true)
    public void sendAutomaticQuestionnaireReminder(Questionnaire questionnaire) {
        if (isQuestionnaireInProgress(questionnaire)) {
            log.info("[questionnaire] Ignorando lembrete automático para questionário {} fora da vigência", questionnaire.getId());
            return;
        }
        Set<String> emails = resolveEmailsForReminder(questionnaire.getProject(), questionnaire.getId(), null);
        if (emails.isEmpty()) {
            log.info("[questionnaire] Nenhum email pendente para lembrete automático questionário {}", questionnaire.getId());
            return;
        }
        QuestionnaireReminderContext context = QuestionnaireReminderContext.from(questionnaire);
        sendReminderEmails(emails, context);
    }

    private boolean isQuestionnaireInProgress(Questionnaire questionnaire) {
        LocalDate today = LocalDate.now();
        boolean withinDates = questionnaire.getApplicationStartDate() != null
                && questionnaire.getApplicationEndDate() != null
                && !today.isBefore(questionnaire.getApplicationStartDate())
                && !today.isAfter(questionnaire.getApplicationEndDate());
        return !withinDates || questionnaire.getStatus() != TimelineStatusEnum.EM_ANDAMENTO;
    }


    private Set<String> resolveEmailsForReminder(Project project, Integer questionnaireId, QuestionnaireReminderRequestDTO request) {
        Set<String> manualEmails = request != null && request.emails() != null
                ? request.emails().stream().filter(Objects::nonNull).collect(Collectors.toSet())
                : Set.of();
        if (!manualEmails.isEmpty()) {
            return manualEmails;
        }
        List<QuestionnaireResponse> pendingResponses = questionnaireResponseRepository.findPendingResponses(project.getId(), questionnaireId);
        Set<Long> pendingRepresentativeIds = pendingResponses.stream()
                .map(QuestionnaireResponse::getRepresentativeId)
                .collect(Collectors.toSet());
        return Optional.ofNullable(project.getRepresentatives()).orElse(Set.of())
                .stream()
                .filter(rep -> pendingRepresentativeIds.contains(rep.getId()))
                .map(rep -> rep.getUser().getEmail())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void sendReminderEmails(Collection<String> emails, QuestionnaireReminderContext context) {
        var distinctEmails = emails == null ? List.<String>of() : emails.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctEmails.isEmpty()) {
            log.info("[sendReminderEmails] Nenhum destinatário para envio de lembrete interno/email questionário={}", context != null ? context.questionnaireId() : null);
            return;
        }

        for (String email : distinctEmails) {
            try {
                notificationDispatcher.dispatchQuestionnaireReminder(email, context);
            } catch (EmailSendingException ex) {
                log.warn("[sendReminderEmails] Falha ao enviar email de lembrete para {} (questionário={}). Seguindo com notificação interna.",
                        email, context != null ? context.questionnaireId() : null, ex);
            } catch (Exception ex) {
                log.warn("[sendReminderEmails] Erro inesperado ao enviar email de lembrete para {} (questionário={}). Seguindo com notificação interna.",
                        email, context != null ? context.questionnaireId() : null, ex);
            }
        }

        var currentUser = currentUserPort.getCurrentUser();
        if (currentUser == null) {
            log.warn("[sendReminderEmails] Usuário atual não encontrado para criar notificação interna. Abortando notificação interna (emails={})", distinctEmails);
            return;
        }

        String senderRoleForTemplate = switch (currentUser.getRole()) {
            case ADMIN -> "PROJECT_MANAGER";
            case USER -> "PROJECT_MANAGER";
        };

        var sender = new NotificationParty(
                currentUser.getId(),
                currentUser.getFirstName() + " " + currentUser.getLastName(),
                currentUser.getEmail(),
                List.of(senderRoleForTemplate)
        );

        for (String email : distinctEmails) {
            Long recipientUserId = userRepository.findByEmail(email).map(User::getId).orElse(null);

            var recipient = new NotificationParty(
                    recipientUserId,
                    null,
                    email,
                    List.of("AGENT")
            );

            log.debug("[sendReminderEmails] Criando notificação interna QUESTIONNAIRE_REMINDER senderUserId={} recipientUserId={} recipientEmail={}",
                    sender.userId(), recipient.userId(), email);

            var saved = sendInternalNotificationUseCase.execute(new SendInternalNotificationCommand(
                    "QUESTIONNAIRE_REMINDER",
                    sender,
                    recipient,
                    Map.of(
                            "recipientName", "",
                            "senderRole", senderRoleForTemplate,
                            "questionnaireName", context.questionnaireName(),
                            "projectName", context.projectName(),
                            "period", context.period()
                    )
            ));

            log.info("[sendReminderEmails] Notificação interna de lembrete criada id={} recipientUserId={} recipientEmail={}",
                    saved != null ? saved.id() : null,
                    recipientUserId,
                    email);
        }
    }
}

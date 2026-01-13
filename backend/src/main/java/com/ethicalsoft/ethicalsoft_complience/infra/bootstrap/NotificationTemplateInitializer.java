package com.ethicalsoft.ethicalsoft_complience.infra.bootstrap;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.NotificationTemplateDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.NotificationTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class NotificationTemplateInitializer {

    private static final String QUESTIONNAIRE_REMINDER = "QUESTIONNAIRE_REMINDER";
    private static final String PASSWORD_RECOVERY = "PASSWORD_RECOVERY";
    private static final String NEW_USER_CREDENTIALS = "NEW_USER_CREDENTIALS";
    private static final String PROJECT_ASSIGNMENT = "PROJECT_ASSIGNMENT";
    private static final String QUESTIONNAIRE_SUBMITTED = "QUESTIONNAIRE_SUBMITTED";
    private static final String QUESTIONNAIRE_COMPLETED = "QUESTIONNAIRE_COMPLETED";
    private static final String DEADLINE_REMINDER = "DEADLINE_REMINDER";

    private final NotificationTemplateRepository repository;

    @PostConstruct
    public void seedTemplates() {
        templatesToSeed().forEach(this::insertIfMissing);
    }

    private List<NotificationTemplateDocument> templatesToSeed() {
        return List.of(
                NotificationTemplateDocument.builder()
                        .key(QUESTIONNAIRE_REMINDER)
                        .whoCanSend(List.of("Analista de Qualidade", "Analista de Requisitos", UserRoleEnum.ADMIN.name()))
                        .recipients(List.of())
                        .title("Questionário pendente: {questionnaireName}")
                        .body("Olá {recipientName}, existe um questionário pendente no projeto {projectName}: {questionnaireName} ({period}).")
                        .templateLink("users/questionnaire-reminder.ftl")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(PASSWORD_RECOVERY)
                        .whoCanSend(List.of(UserRoleEnum.USER.name(), UserRoleEnum.ADMIN.name()))
                        .recipients(List.of())
                        .title("")
                        .body("")
                        .templateLink("recover/recovery-email.ftl")
                        .channels(List.of(NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(NEW_USER_CREDENTIALS)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name()))
                        .recipients(List.of())
                        .title("")
                        .body("")
                        .templateLink("users/new-user-credentials.ftl")
                        .channels(List.of(NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(PROJECT_ASSIGNMENT)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name()))
                        .recipients(List.of())
                        .title("Você foi vinculado ao projeto {projectName}")
                        .body("Olá {firstName}, você foi vinculado ao projeto {projectName}. Responsável: {adminName} ({adminEmail}). Papéis: {roles}. Período: {startDateFormatted} até {deadlineFormatted}. Próximos passos: {timelineSummary} / Próximo questionário: {nextQuestionnaireFormatted}.")
                        .templateLink("users/project-assignment.ftl")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(QUESTIONNAIRE_SUBMITTED)
                        .whoCanSend(List.of(UserRoleEnum.USER.name(), UserRoleEnum.ADMIN.name()))
                        .recipients(List.of(UserRoleEnum.ADMIN.name()))
                        .title("Questionário enviado: {questionnaireName}")
                        .body("{responderName} respondeu o questionário {questionnaireName} no projeto {projectName} em {submittedAtFormatted}.")
                        .templateLink("users/questionnaire-submitted.ftl")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(QUESTIONNAIRE_COMPLETED)
                        .whoCanSend(List.of(UserRoleEnum.USER.name(), UserRoleEnum.ADMIN.name()))
                        .recipients(List.of())
                        .title("Questionário finalizado: {questionnaireName}")
                        .body("Questionário {questionnaireName} do projeto {projectName} foi finalizado/aprovado em {finishedAtFormatted} por {approvedBy}.")
                        .templateLink("users/questionnaire-completed.ftl")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(DEADLINE_REMINDER)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name()))
                        .recipients(List.of())
                        .title("Prazo próximo: {projectName}")
                        .body("O projeto {projectName} vence em {deadlineFormatted} (faltam {daysRemaining} dia(s)).")
                        .templateLink("users/project-deadline-reminder.ftl")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build()
        );
    }

    private void insertIfMissing(NotificationTemplateDocument template) {
        if (template == null || template.getKey() == null || template.getKey().isBlank()) {
            return;
        }
        if (repository.findByKey(template.getKey()).isPresent()) {
            return;
        }
        repository.save(template);
    }
}

package com.ethicalsoft.ethicalsoft_complience.infra.bootstrap;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.NotificationTemplateDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.NotificationTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
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
    private static final String QUESTIONNAIRE_OVERDUE = "QUESTIONNAIRE_OVERDUE";
    private static final String QUESTIONNAIRE_ISEP_CALCULATED = "QUESTIONNAIRE_ISEP_CALCULATED";
    private static final String PROJECT_UNASSIGNMENT = "PROJECT_UNASSIGNMENT";
    private static final String REPRESENTATIVE_EMAIL_CHANGED = "REPRESENTATIVE_EMAIL_CHANGED";
    private static final String PROJECT_UPDATED = "PROJECT_UPDATED";
    private static final String QUESTIONNAIRE_RESCHEDULED = "QUESTIONNAIRE_RESCHEDULED";
    private static final String PROJECT_DEADLINE_EXCEEDED_WARNING = "PROJECT_DEADLINE_EXCEEDED_WARNING";
    private static final String NEXT_QUESTIONNAIRE_STARTING_SOON = "NEXT_QUESTIONNAIRE_STARTING_SOON";

    private final NotificationTemplateRepository repository;

    @PostConstruct
    public void seedTemplates() {
        try {
            templatesToSeed().forEach(this::insertIfMissing);
        } catch (Exception e) {
            log.warn("[notification-template-init] Não foi possível inicializar templates de notificação no MongoDB. " +
                    "A aplicação continuará normalmente. Erro: {}", e.getMessage());
        }
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
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(QUESTIONNAIRE_OVERDUE)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name(), UserRoleEnum.USER.name()))
                        .recipients(List.of())
                        .title("Questionário atrasado: {questionnaireName}")
                        .body("O questionário {questionnaireName} do projeto {projectName} expirou em {expiredAtFormatted} sem 100% de respostas. Respondentes pendentes: {pendingCount}/{totalCount}.")
                        .templateLink("users/questionnaire-overdue.ftl")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(QUESTIONNAIRE_ISEP_CALCULATED)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name(), UserRoleEnum.USER.name()))
                        .recipients(List.of())
                        .title("ISEP calculado: {questionnaireName} — Faixa {band}")
                        .body("O ISEP do questionário {questionnaireName} no projeto {projectName} foi calculado: {isepPercent}% (Faixa {band}). Encerrado por: {closedBy}.")
                        .templateLink("users/questionnaire-isep-calculated.ftl")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(PROJECT_UNASSIGNMENT)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name(), "SYSTEM"))
                        .recipients(List.of())
                        .title("Você foi removido do projeto {projectName}")
                        .body("Olá {firstName}, você foi removido do projeto {projectName} pelo administrador {adminName}. Caso tenha dúvidas, entre em contato com o administrador do projeto.")
                        .templateLink("users/project-unassignment.ftl")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(REPRESENTATIVE_EMAIL_CHANGED)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name(), "SYSTEM"))
                        .recipients(List.of())
                        .title("Alteração de email no projeto {projectName}")
                        .body("O email de acesso ao projeto {projectName} foi alterado de {oldEmail} para {newEmail}. Se você não reconhece esta alteração, entre em contato com o administrador.")
                        .templateLink("")
                        .channels(List.of(NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(PROJECT_UPDATED)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name(), "SYSTEM"))
                        .recipients(List.of())
                        .title("Projeto atualizado: {projectName}")
                        .body("O projeto {projectName} foi atualizado pelo administrador. Verifique se há novas perguntas ou alterações nos questionários.")
                        .templateLink("")
                        .channels(List.of(NotificationChannel.INTERNAL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(QUESTIONNAIRE_RESCHEDULED)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name(), "SYSTEM"))
                        .recipients(List.of())
                        .title("Questionário reagendado: {questionnaireName}")
                        .body("O questionário {questionnaireName} do projeto {projectName} foi reagendado. Novo período: {newStartDate} até {newEndDate}.")
                        .templateLink("users/questionnaire-rescheduled.ftl")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(PROJECT_DEADLINE_EXCEEDED_WARNING)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name(), "SYSTEM"))
                        .recipients(List.of())
                        .title("⚠️ Prazo do projeto excedido: {projectName}")
                        .body("O reagendamento do questionário {questionnaireName} faz com que a data de término ({newEndDate}) ultrapasse o prazo do projeto ({deadline}). Considere estender o prazo do projeto ou reduzir a duração das próximas etapas/iterações.")
                        .templateLink("")
                        .channels(List.of(NotificationChannel.INTERNAL.name(), NotificationChannel.EMAIL.name()))
                        .build(),
                NotificationTemplateDocument.builder()
                        .key(NEXT_QUESTIONNAIRE_STARTING_SOON)
                        .whoCanSend(List.of(UserRoleEnum.ADMIN.name(), "SYSTEM"))
                        .recipients(List.of())
                        .title("Próximo questionário: {nextQuestionnaireName} — Início em {daysUntilStart} dia(s)")
                        .body("O questionário {closedQuestionnaireName} foi encerrado. O próximo questionário {nextQuestionnaireName} do projeto {projectName} iniciará em {nextStartDate}. Caso deseje adiantar o início, utilize o reagendamento no painel do projeto.")
                        .templateLink("users/next-questionnaire-starting-soon.ftl")
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

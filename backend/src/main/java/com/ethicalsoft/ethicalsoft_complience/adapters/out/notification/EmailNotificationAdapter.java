package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.application.port.NotificationDispatcherPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.dto.NewUserCredentialsNotificationDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.dto.ProjectAssignmentNotificationDTO;
import com.ethicalsoft.ethicalsoft_complience.exception.EmailSendingException;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.criteria.QuestionnaireReminderContext;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationAdapter implements NotificationDispatcherPort {

    private static final DateTimeFormatter EMAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final FreeMarkerConfigurer freemarkerConfig;
    private final QuestionnaireRepository questionnaireRepository;

    @Value("${app.frontend.url:https://app.ethicalsoft.com}")
    private String frontendUrl;

    @Value("${app.environment:local}")
    private String environment;

    @Value("${app.support.email:suporte@ethicalsoft.com}")
    private String supportEmail;

    @Override
    public void dispatchRecoveryCode(String to, String code) {
        try {
            log.info("[notification-email] Enviando código de recuperação para {}", to);
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);

            Map<String, Object> model = new HashMap<>();
            model.put("code", code);
            String html = generateEmailBody( model );

            helper.setTo( to );
            helper.setSubject( "Password Recovery Code" );
            helper.setText( html, true );

            mailSender.send( message );
            log.info("[notification-email] Código de recuperação enviado para {}", to);
        } catch ( MessagingException | IOException | TemplateException e ) {
            log.error("[notification-email] Falha ao enviar email de recuperação para {}", to, e);
            throw new EmailSendingException( "Failed to send recovery email to " + to, e );
        }
    }

    @Override
    @Async
    public void dispatchNewUserCredentials(NewUserCredentialsNotificationDTO dto) {
        try {
            log.info("[notification-email] Enviando credenciais temporárias para {}", dto.to());
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);

            Map<String, Object> model = new HashMap<>();
            model.put("firstName", dto.firstName());
            model.put("temporaryPassword", "******");
            model.put("projectName", dto.projectName());
            model.put("adminName", dto.adminName());
            model.put("resetLink", buildFrontendLink("/auth/reset-password"));
            model.put("supportEmail", supportEmail);
            model.put("environment", environment);
            String html = generateEmailBody("users/new-user-credentials.ftl", model);

            helper.setTo( dto.to() );
            helper.setSubject( "Bem-vindo ao EthicalSoft Compliance" );
            helper.setText( html, true );

            mailSender.send( message );
            log.info("[notification-email] Credenciais temporárias enviadas para {}", dto.to());
        } catch ( MessagingException | IOException | TemplateException e ) {
            log.error("[notification-email] Falha ao enviar credenciais temporárias para {}", dto.to(), e );
            throw new EmailSendingException( "Falha ao enviar credenciais para " + dto.to(), e );
        }
    }

    @Override
    @Async
    public void dispatchProjectAssignment(ProjectAssignmentNotificationDTO dto) {
        try {
            log.info("[notification-email] Enviando notificação de vinculação ao projeto {} para {}", dto.projectName(), dto.to());
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper( message, true );

            Map<String, Object> model = new HashMap<>();
            var activeContext = buildProjectQuestionnaireContext(dto.projectId());
            model.put( "firstName", dto.firstName() );
            model.put( "projectName", dto.projectName() );
            model.put( "projectLink", buildFrontendLink( "/projects/" + dto.projectId() ) );
            model.put( "adminName", dto.adminName() );
            model.put( "adminEmail", dto.adminEmail() );
            model.put( "roles", dto.roleNames() );
            model.put( "timelineSummary", dto.timelineSummary() );
            model.put( "startDateFormatted", formatDate( dto.startDate() ) );
            model.put( "deadlineFormatted", formatDate( dto.deadline() ) );
            model.put( "nextQuestionnaireFormatted", formatDate( dto.nextQuestionnaireDate() ) );
            model.put( "supportEmail", supportEmail );
            model.put( "environment", environment );
            model.put( "activeQuestionnairesMessage", activeContext != null ? activeContext.activeMessage() : null );
            String html = generateEmailBody( "users/project-assignment.ftl", model );

            helper.setTo( dto.to() );
            helper.setSubject( "Você foi vinculado a um novo projeto" );
            helper.setText( html, true );

            mailSender.send( message );
            log.info("[notification-email] Notificação de projeto enviada para {}", dto.to());
        } catch ( MessagingException | IOException | TemplateException e ) {
            log.error("[notification-email] Falha ao enviar notificação de projeto para {}", dto.to(), e );
            throw new EmailSendingException( "Falha ao enviar notificação de projeto para " + dto.to(), e );
        }
    }

    @Override
    public void dispatchQuestionnaireReminder(String to, QuestionnaireReminderContext context) {
        try {
            log.info("[notification-email] Enviando lembrete de questionário {} para {}", context.questionnaireId(), to);
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);

            Map<String, Object> model = new HashMap<>();
            model.put("questionnaireName", context.questionnaireName());
            model.put("period", context.period());
            model.put("projectName", context.projectName());
            model.put("projectLink", buildFrontendLink("/projects/" + context.projectId()));

            String html = generateEmailBody("users/questionnaire-reminder.ftl", model);

            helper.setTo(to);
            helper.setSubject("Questionário disponível - " + context.projectName());
            helper.setText(html, true);

            mailSender.send(message);
        } catch ( MessagingException | IOException | TemplateException e ) {
            log.error("[notification-email] Falha ao enviar lembrete de questionário {} para {}", context.questionnaireId(), to, e);
            throw new EmailSendingException("Falha ao enviar lembrete para " + to, e);
        }
    }

    public ProjectQuestionnaireContext buildProjectQuestionnaireContext(Long projectId) {
        List<Questionnaire> active = questionnaireRepository.findActiveByProjectAndDate(projectId, LocalDate.now());
        if (active.isEmpty()) {
            return null;
        }
        return new ProjectQuestionnaireContext(active);
    }

    private String buildFrontendLink( String path ) {
        return frontendUrl.endsWith( "/" ) ? frontendUrl.substring( 0, frontendUrl.length() - 1 ) + path : frontendUrl + path;
    }

    private String formatDate( LocalDate date ) {
        return date == null ? "a ser comunicado" : EMAIL_DATE_FORMAT.format( date );
    }

    private String generateEmailBody( Map<String, Object> model ) throws IOException, TemplateException {
        var template = freemarkerConfig.getConfiguration().getTemplate( "recover/recovery-email.ftl" );
        return FreeMarkerTemplateUtils.processTemplateIntoString( template, model );
    }

    private String generateEmailBody( String templatePath, Map<String, Object> model ) throws IOException, TemplateException {
        var template = freemarkerConfig.getConfiguration().getTemplate( templatePath );
        return FreeMarkerTemplateUtils.processTemplateIntoString( template, model );
    }

    public record ProjectQuestionnaireContext(List<Questionnaire> activeQuestionnaires) {
        public String activeMessage() {
            if (activeQuestionnaires == null || activeQuestionnaires.isEmpty()) {
                return null;
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return activeQuestionnaires.stream()
                    .map(q -> q.getName() + " (" + formatter.format(q.getApplicationStartDate()) + " - " + formatter.format(q.getApplicationEndDate()) + ")")
                    .collect(Collectors.joining(", "));
        }
    }
}
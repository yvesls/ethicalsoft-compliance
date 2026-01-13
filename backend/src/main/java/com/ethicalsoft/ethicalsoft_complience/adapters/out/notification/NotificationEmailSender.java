package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.exception.EmailSendingException;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailSender {

    private final JavaMailSender mailSender;
    private final FreeMarkerConfigurer freemarkerConfig;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    public void send(String to, String subject, String templatePath, Map<String, Object> model) {
        if (!emailEnabled) {
            log.info("[notification-email] Envio desabilitado (app.email.enabled=false). Ignorando envio para {}", to);
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);

            var template = freemarkerConfig.getConfiguration().getTemplate(templatePath);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model == null ? Map.of() : model);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException | IOException | TemplateException e) {
            log.error("[notification-email] Falha ao enviar email template={} para {}", templatePath, to, e);
            throw new EmailSendingException("Falha ao enviar email para " + to, e);
        }
    }
}


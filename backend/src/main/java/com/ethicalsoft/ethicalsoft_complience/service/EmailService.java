package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.EmailSendingException;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;
	private final FreeMarkerConfigurer freemarkerConfig;

	@Value("${app.frontend.url:https://app.ethicalsoft.com}")
	private String frontendUrl;

	public void sendRecoveryEmail( String to, String code ) {
		try {
			var message = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(message, true);

			Map<String, Object> model = new HashMap<>();
			model.put("code", code);
			String html = generateEmailBody( model );

			helper.setTo( to );
			helper.setSubject( "Password Recovery Code" );
			helper.setText( html, true );

			mailSender.send( message );
		} catch ( MessagingException | IOException | TemplateException e ) {
			throw new EmailSendingException( "Failed to send recovery email to " + to, e );
		}
	}

	@Async
	public void sendNewUserCredentialsEmail( String to, String firstName, String tempPassword ) {
		try {
			var message = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(message, true);

			Map<String, Object> model = new HashMap<>();
			model.put("firstName", firstName);
			model.put("temporaryPassword", tempPassword);
			model.put("resetLink", frontendUrl + "/auth/reset-password");
			String html = generateEmailBody("users/new-user-credentials.ftl", model);

			helper.setTo( to );
			helper.setSubject( "Bem-vindo ao EthicalSoft Compliance" );
			helper.setText( html, true );

			mailSender.send( message );
		} catch ( MessagingException | IOException | TemplateException e ) {
			throw new EmailSendingException( "Falha ao enviar credenciais para " + to, e );
		}
	}

	private String generateEmailBody( Map<String, Object> model ) throws IOException, TemplateException {
		var template = freemarkerConfig.getConfiguration().getTemplate( "recover/recovery-email.ftl" );
		return FreeMarkerTemplateUtils.processTemplateIntoString( template, model );
	}

	private String generateEmailBody( String templatePath, Map<String, Object> model ) throws IOException, TemplateException {
		var template = freemarkerConfig.getConfiguration().getTemplate( templatePath );
		return FreeMarkerTemplateUtils.processTemplateIntoString( template, model );
	}
}

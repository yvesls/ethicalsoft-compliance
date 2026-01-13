package com.ethicalsoft.ethicalsoft_complience.application.usecase.notification;

import com.ethicalsoft.ethicalsoft_complience.application.port.notification.InternalNotificationPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTemplatePort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendInternalNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SendInternalNotificationUseCaseTest {

    @Test
    void deveGerarNotificacaoInternaComPlaceHoldersResolvidos() {
        NotificationTemplatePort templatePort = mock(NotificationTemplatePort.class);
        InternalNotificationPort internalPort = mock(InternalNotificationPort.class);
        ResolvePlaceholderPolicy policy = new ResolvePlaceholderPolicy();

        when(templatePort.findByKey("QUESTIONNAIRE_REMINDER"))
                .thenReturn(Optional.of(new NotificationTemplate(
                        "QUESTIONNAIRE_REMINDER",
                        List.of("QUALITY_ANALYST"),
                        List.of("AGENT"),
                        "Pendência: {questionnaireName}",
                        "Projeto {projectName}",
                        null,
                        List.of(com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationChannel.INTERNAL)
                )));
        when(internalPort.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            return new Notification(
                    "id-1",
                    n.sender(),
                    n.recipient(),
                    n.title(),
                    n.content(),
                    n.status(),
                    n.createdAt() != null ? n.createdAt() : LocalDateTime.now(),
                    n.updatedAt(),
                    n.templateKey()
            );
        });

        var useCase = new SendInternalNotificationUseCase(templatePort, internalPort, policy);

        var cmd = new SendInternalNotificationCommand(
                "QUESTIONNAIRE_REMINDER",
                new NotificationParty(1L, "QA", "qa@x.com", List.of("QUALITY_ANALYST")),
                new NotificationParty(2L, "Agente", "a@x.com", List.of("AGENT")),
                Map.of("questionnaireName", "Q1", "projectName", "P1")
        );

        Notification saved = useCase.execute(cmd);

        assertEquals("id-1", saved.id());
        assertEquals(NotificationStatus.UNREAD, saved.status());
        assertEquals("Pendência: Q1", saved.title());
        assertEquals("Projeto P1", saved.content());
    }

    @Test
    void deveNegarQuandoRoleNaoPermitidaNoTemplate() {
        NotificationTemplatePort templatePort = mock(NotificationTemplatePort.class);
        InternalNotificationPort internalPort = mock(InternalNotificationPort.class);
        ResolvePlaceholderPolicy policy = new ResolvePlaceholderPolicy();

        when(templatePort.findByKey("QUESTIONNAIRE_REMINDER"))
                .thenReturn(Optional.of(new NotificationTemplate(
                        "QUESTIONNAIRE_REMINDER",
                        List.of("PROJECT_MANAGER"),
                        List.of("AGENT"),
                        "T", "B",
                        null,
                        List.of(com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationChannel.INTERNAL)
                )));
        var useCase = new SendInternalNotificationUseCase(templatePort, internalPort, policy);

        var cmd = new SendInternalNotificationCommand(
                "QUESTIONNAIRE_REMINDER",
                new NotificationParty(1L, "QA", "qa@x.com", List.of("QUALITY_ANALYST")),
                new NotificationParty(2L, "Agente", "a@x.com", List.of("AGENT")),
                Map.of()
        );

        assertThrows(AccessDeniedException.class, () -> useCase.execute(cmd));
        verify(internalPort, never()).save(any());
    }
}

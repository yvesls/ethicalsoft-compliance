package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendQuestionnaireReminderUseCase {

    private final SendNotificationUseCase sendNotificationUseCase;

    public void execute(Long projectId) {
        sendNotificationUseCase.execute(new com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand(
                NotificationType.QUESTIONNAIRE_REMINDER,
                java.util.Map.of("projectId", projectId)
        ));
    }
}

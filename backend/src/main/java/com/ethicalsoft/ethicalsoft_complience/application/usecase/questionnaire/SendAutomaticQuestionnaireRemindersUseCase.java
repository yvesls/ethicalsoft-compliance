package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendAutomaticQuestionnaireRemindersUseCase {

    private final QuestionnaireRepository questionnaireRepository;

    private final SendNotificationUseCase sendNotificationUseCase;

    public void execute() {
        var today = LocalDate.now();
        var starting = questionnaireRepository.findQuestionnairesStartingToday(today);
        starting.forEach(this::trySend);
    }

    private void trySend(Questionnaire questionnaire) {
        try {
            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.QUESTIONNAIRE_REMINDER,
                    java.util.Map.of("projectId", questionnaire.getProject().getId())
            ));
        } catch (Exception ex) {
            log.error("[questionnaire-reminder] Falha ao enviar lembrete automático questionário={}", questionnaire != null ? questionnaire.getId() : null, ex);
        }
    }
}

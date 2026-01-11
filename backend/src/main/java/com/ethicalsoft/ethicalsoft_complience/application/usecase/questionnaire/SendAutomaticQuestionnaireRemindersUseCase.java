package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireReminderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendAutomaticQuestionnaireRemindersUseCase {

    private final QuestionnaireRepository questionnaireRepository;

    private final QuestionnaireReminderPort questionnaireReminderPort;

    public void execute() {
        var today = LocalDate.now();
        var starting = questionnaireRepository.findQuestionnairesStartingToday(today);
        starting.forEach(this::trySend);
    }

    private void trySend(Questionnaire questionnaire) {
        try {
            questionnaireReminderPort.sendAutomaticReminder(
                    questionnaire.getProject().getId(),
                    questionnaire.getId()
            );
        } catch (Exception ex) {
            log.error("[questionnaire-reminder] Falha ao enviar lembrete automático questionário={}", questionnaire != null ? questionnaire.getId() : null, ex);
        }
    }
}

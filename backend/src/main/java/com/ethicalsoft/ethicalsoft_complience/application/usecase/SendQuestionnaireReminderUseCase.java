package com.ethicalsoft.ethicalsoft_complience.application.usecase;

import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireReminderPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireReminderRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendQuestionnaireReminderUseCase {

    private final QuestionnaireReminderPort questionnaireReminderPort;

    public void execute(Long projectId, Integer questionnaireId, QuestionnaireReminderRequestDTO requestDTO) {
        questionnaireReminderPort.sendReminder(projectId, questionnaireId, requestDTO);
    }
}

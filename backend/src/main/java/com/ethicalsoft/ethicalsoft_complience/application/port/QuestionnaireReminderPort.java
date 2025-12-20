package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireReminderRequestDTO;

public interface QuestionnaireReminderPort {
    void sendReminder(Long projectId, Integer questionnaireId, QuestionnaireReminderRequestDTO request);
}


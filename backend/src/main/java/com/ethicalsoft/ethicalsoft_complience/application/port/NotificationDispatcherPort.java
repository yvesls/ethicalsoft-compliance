package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.application.port.dto.NewUserCredentialsNotificationDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.dto.ProjectAssignmentNotificationDTO;
import com.ethicalsoft.ethicalsoft_complience.service.criteria.QuestionnaireReminderContext;

public interface NotificationDispatcherPort {
    void dispatchRecoveryCode(String to, String code);
    void dispatchNewUserCredentials(NewUserCredentialsNotificationDTO dto);
    void dispatchProjectAssignment(ProjectAssignmentNotificationDTO dto);
    void dispatchQuestionnaireReminder(String to, QuestionnaireReminderContext context);
}
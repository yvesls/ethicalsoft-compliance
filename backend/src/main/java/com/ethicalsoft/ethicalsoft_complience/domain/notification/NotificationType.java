package com.ethicalsoft.ethicalsoft_complience.domain.notification;

public enum NotificationType {
    QUESTIONNAIRE_REMINDER("QUESTIONNAIRE_REMINDER"),
    PASSWORD_RECOVERY("PASSWORD_RECOVERY"),
    NEW_USER_CREDENTIALS("NEW_USER_CREDENTIALS"),
    PROJECT_ASSIGNMENT("PROJECT_ASSIGNMENT"),
    QUESTIONNAIRE_SUBMITTED("QUESTIONNAIRE_SUBMITTED"),
    QUESTIONNAIRE_COMPLETED("QUESTIONNAIRE_COMPLETED"),
    DEADLINE_REMINDER("DEADLINE_REMINDER"),
    NOTIFICATION_DEFAULT("NOTIFICATION_DEFAULT");

    private final String templateKey;

    NotificationType(String templateKey) {
        this.templateKey = templateKey;
    }

    public String templateKey() {
        return templateKey;
    }
}

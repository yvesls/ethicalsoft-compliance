package com.ethicalsoft.ethicalsoft_complience.service.criteria;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Questionnaire;

import java.time.format.DateTimeFormatter;

public record QuestionnaireReminderContext(
        Long projectId,
        Integer questionnaireId,
        String questionnaireName,
        String period,
        String projectName
) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static QuestionnaireReminderContext from(Questionnaire questionnaire) {
        String period = "";
        if (questionnaire.getApplicationStartDate() != null && questionnaire.getApplicationEndDate() != null) {
            period = DATE_FORMAT.format(questionnaire.getApplicationStartDate()) + " até " + DATE_FORMAT.format(questionnaire.getApplicationEndDate());
        }
        return new QuestionnaireReminderContext(
                questionnaire.getProject().getId(),
                questionnaire.getId(),
                questionnaire.getName(),
                period,
                questionnaire.getProject().getName()
        );
    }
}


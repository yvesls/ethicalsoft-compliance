package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request;

import jakarta.validation.constraints.Email;

import java.util.List;

public record QuestionnaireReminderRequestDTO(
        List<@Email(message = "Email inválido") String> emails
) {
}

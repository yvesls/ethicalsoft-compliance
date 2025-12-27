package com.ethicalsoft.ethicalsoft_complience.application.port.dto;

import java.time.LocalDate;
import java.util.List;

public record ProjectAssignmentNotificationDTO(
        String to,
        String firstName,
        String projectName,
        Long projectId,
        String adminName,
        String adminEmail,
        List<String> roleNames,
        String timelineSummary,
        LocalDate startDate,
        LocalDate deadline,
        LocalDate nextQuestionnaireDate
) {}
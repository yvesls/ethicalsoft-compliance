package com.ethicalsoft.ethicalsoft_complience.application.port.dto;

public record NewUserCredentialsNotificationDTO(
        String to,
        String firstName,
        String tempPassword,
        String projectName,
        String adminName
) {}
package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

public record ConsolidatedAnswersFilter(
        Long representativeId,
        Long questionId,
        Long roleId,
        String roleName,
        Boolean response,
        String questionText
) {}
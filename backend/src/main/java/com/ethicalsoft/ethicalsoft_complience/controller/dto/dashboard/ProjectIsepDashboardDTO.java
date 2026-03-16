package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record ProjectIsepDashboardDTO(
        Long projectId,
        String projectName,
        String projectType,
        BigDecimal projectIsep,
        BigDecimal projectIsepPercent,
        String projectBand,
        List<IsepHistoryItemDTO> isepHistory,
        int totalQuestionnaires,
        int completedQuestionnaires
) {}


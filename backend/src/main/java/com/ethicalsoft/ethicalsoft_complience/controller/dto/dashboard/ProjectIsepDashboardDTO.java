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
        BigDecimal teamSimpleAveragePercent,
        BigDecimal teamStandardDeviationPercent,
        List<IsepHistoryItemDTO> isepHistory,
        int totalQuestionnaires,
        int completedQuestionnaires,
        BigDecimal ethicsScorePercent,
        BigDecimal processScorePercent,
        BigDecimal fairnessScorePercent,
        BigDecimal esgScorePercent,
        BigDecimal ethicsDebtPercent,
        BigDecimal techDebtPercent
) {}


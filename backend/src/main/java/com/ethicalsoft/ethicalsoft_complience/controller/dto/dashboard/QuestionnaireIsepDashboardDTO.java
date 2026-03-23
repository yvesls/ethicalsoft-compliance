package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record QuestionnaireIsepDashboardDTO(
        Integer questionnaireId,
        String questionnaireName,
        String stageName,
        String iterationName,
        BigDecimal isep,
        BigDecimal isepPercent,
        String band,
        BigDecimal teamSimpleAverage,
        BigDecimal teamSimpleAveragePercent,
        BigDecimal teamStandardDeviation,
        BigDecimal teamStandardDeviationPercent,
        LocalDateTime calculatedAt,
        Map<String, Long> bandDistribution,
        List<MemberComplianceDTO> memberResults,
        List<StageComplianceDTO> stageResults,
        List<String> justificationTexts,
        BigDecimal ethicsScorePercent,
        BigDecimal processScorePercent,
        BigDecimal fairnessScorePercent,
        BigDecimal esgScorePercent,
        BigDecimal ethicsDebtPercent,
        BigDecimal techDebtPercent
) {}


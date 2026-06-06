package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record IsepDataExportDTO(
        Long projectId,
        String projectName,
        Integer questionnaireId,
        String questionnaireName,
        String iterationOrStageName,
        LocalDateTime calculatedAt,
        BigDecimal isepPercent,
        String band,
        BigDecimal teamAveragePercent,
        BigDecimal standardDeviationPercent,
        BigDecimal ethicsScorePercent,
        BigDecimal processScorePercent,
        BigDecimal fairnessScorePercent,
        BigDecimal esgScorePercent,
        BigDecimal ethicsDebtPercent,
        BigDecimal techDebtPercent,
        List<MemberExportRow> members
) {

    public record MemberExportRow(
            Long representativeId,
            String memberName,
            BigDecimal icpPercent,
            String band,
            List<StageIemExportRow> stageResults
    ) {}

    public record StageIemExportRow(
            Integer stageId,
            String stageName,
            BigDecimal iemPercent
    ) {}
}


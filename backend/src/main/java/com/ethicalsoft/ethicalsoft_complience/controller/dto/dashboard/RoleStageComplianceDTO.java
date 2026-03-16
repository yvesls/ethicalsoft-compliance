package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.math.BigDecimal;
import java.util.Map;

public record RoleStageComplianceDTO(
        Long roleId,
        String roleName,
        Map<Integer, StageIemSummary> iemByStage
) {

    public record StageIemSummary(
            Integer stageId,
            String stageName,
            BigDecimal iemPercent,
            String band,
            int memberCount
    ) {}
}


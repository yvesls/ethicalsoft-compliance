package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.math.BigDecimal;

public record StageComplianceDTO(
        Long representativeId,
        String representativeName,
        Integer stageId,
        String stageName,
        BigDecimal iem,
        BigDecimal iemPercent,
        String band
) {}


package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IsepHistoryItemDTO(
        Integer questionnaireId,
        String questionnaireName,
        String stageName,
        String iterationName,
        BigDecimal iseq,
        BigDecimal iseqPercent,
        String band,
        LocalDateTime calculatedAt,
        BigDecimal ethicsDebtPercent,
        BigDecimal techDebtPercent
) {}


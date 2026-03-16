package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IsepHistoryItemDTO(
        Integer questionnaireId,
        String questionnaireName,
        String stageName,
        String iterationName,
        BigDecimal isep,
        BigDecimal isepPercent,
        String band,
        LocalDateTime calculatedAt
) {}


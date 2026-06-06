package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record IndividualDashboardDTO(
        Long representativeId,
        String representativeName,
        BigDecimal personalIcp,
        BigDecimal personalIcpPercent,
        String personalBand,
        BigDecimal personalHistoricalAverage,
        BigDecimal personalHistoricalAveragePercent,
        BigDecimal teamAnonymousAverage,
        BigDecimal teamAnonymousAveragePercent,
        List<IsepHistoryItemDTO> personalEvolution
) {}


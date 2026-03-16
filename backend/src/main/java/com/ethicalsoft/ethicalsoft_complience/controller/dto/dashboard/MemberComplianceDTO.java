package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.math.BigDecimal;

public record MemberComplianceDTO(
        Long representativeId,
        String representativeName,
        BigDecimal icp,
        BigDecimal icpPercent,
        String band
) {}


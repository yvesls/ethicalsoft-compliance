package com.ethicalsoft.ethicalsoft_complience.domain.isep;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record IseqCalculationResult(
        Long projectId,
        Integer questionnaireId,
        Map<Long, BigDecimal> memberComplianceIndex,
        Map<Long, Map<Integer, BigDecimal>> memberStageComplianceIndex,
        BigDecimal iseq,
        EthicalComplianceBand iseqBand,
        BigDecimal teamSimpleAverage,
        BigDecimal teamStandardDeviation,
        Map<EthicalComplianceBand, Long> bandDistribution,
        List<String> justificationTexts,
        DomainScores domainScores
) {
}

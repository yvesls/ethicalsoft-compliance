package com.ethicalsoft.ethicalsoft_complience.domain.isep;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record IsepCalculationResult(
        Long projectId,
        Integer questionnaireId,
        Map<Long, BigDecimal> memberPersonalComplianceIndex,
        Map<Long, Map<Integer, BigDecimal>> memberStageComplianceIndex,
        BigDecimal questionnaireIsep,
        EthicalComplianceBand questionnaireBand,
        BigDecimal teamSimpleAverage,
        BigDecimal teamStandardDeviation,
        Map<EthicalComplianceBand, Long> bandDistribution,
        List<String> justificationTexts
) {
}


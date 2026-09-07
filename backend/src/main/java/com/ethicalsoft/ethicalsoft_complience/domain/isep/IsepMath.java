package com.ethicalsoft.ethicalsoft_complience.domain.isep;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class IsepMath {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private IsepMath() {}

    public static BigDecimal weightedAverage(List<WeightedValue> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;

        BigDecimal weightedSum = values.stream()
                .map(v -> v.value().multiply(v.weight()))
                .reduce(BigDecimal.ZERO, (accumulator, value) -> accumulator.add(value));

        BigDecimal totalWeight = values.stream()
                .map(w -> w.weight())
                .reduce(BigDecimal.ZERO, (accumulator, value) -> accumulator.add(value));

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return weightedSum.divide(totalWeight, SCALE, ROUNDING);
    }

    public static BigDecimal simpleAverage(Collection<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, (accumulator, value) -> accumulator.add(value));
        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, ROUNDING);
    }

    public static BigDecimal standardDeviation(Collection<BigDecimal> values) {
        if (values == null || values.size() < 2) return BigDecimal.ZERO;

        BigDecimal mean = simpleAverage(values);
        BigDecimal sumSquaredDiffs = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, (accumulator, value) -> accumulator.add(value));

        BigDecimal variance = sumSquaredDiffs.divide(BigDecimal.valueOf(values.size()), SCALE, ROUNDING);
        return variance.sqrt(new MathContext(SCALE, ROUNDING));
    }

    public static BigDecimal ratio(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), SCALE, ROUNDING);
    }

    public static BigDecimal toPercent(BigDecimal ratio) {
        if (ratio == null) return BigDecimal.ZERO;
        return ratio.multiply(BigDecimal.valueOf(100)).setScale(2, ROUNDING);
    }

    public static Map<EthicalComplianceBand, Long> bandDistribution(Collection<BigDecimal> icpValues) {
        return icpValues.stream()
                .collect(Collectors.groupingBy(
                        icp -> EthicalComplianceBand.classify(toPercent(icp)),
                        Collectors.counting()
                ));
    }

    public record WeightedValue(BigDecimal value, BigDecimal weight) {}
}


package com.ethicalsoft.ethicalsoft_complience.domain.isep;

import java.math.BigDecimal;

public enum EthicalComplianceBand {

    A("Excelente", new BigDecimal("90.00"), new BigDecimal("100.00")),
    B("Bom", new BigDecimal("75.00"), new BigDecimal("89.99")),
    C("Regular", new BigDecimal("60.00"), new BigDecimal("74.99")),
    D("Insuficiente", new BigDecimal("45.00"), new BigDecimal("59.99")),
    E("Crítico", BigDecimal.ZERO, new BigDecimal("44.99"));

    public static final EthicalComplianceBand MINIMUM_ACCEPTABLE = B;

    private final String label;
    private final BigDecimal minInclusive;
    private final BigDecimal maxInclusive;

    EthicalComplianceBand(String label, BigDecimal minInclusive, BigDecimal maxInclusive) {
        this.label = label;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public boolean meetsMinimum() {
        return this.ordinal() <= MINIMUM_ACCEPTABLE.ordinal();
    }

    public static boolean meetsMinimum(String bandName) {
        if (bandName == null || bandName.isBlank()) return false;
        try {
            return EthicalComplianceBand.valueOf(bandName.trim().toUpperCase()).meetsMinimum();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public BigDecimal getMinInclusive() {
        return minInclusive;
    }

    public static EthicalComplianceBand classify(BigDecimal index) {
        if (index == null) return E;
        BigDecimal normalised = index.compareTo(BigDecimal.valueOf(1)) <= 0
                ? index.multiply(BigDecimal.valueOf(100))
                : index;
        for (EthicalComplianceBand band : values()) {
            if (normalised.compareTo(band.minInclusive) >= 0 && normalised.compareTo(band.maxInclusive) <= 0) {
                return band;
            }
        }
        return E;
    }

    public String getLabel() {
        return label;
    }
}


package com.ethicalsoft.ethicalsoft_complience.application.usecase.document;

import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public final class DocumentFormatUtil {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private DocumentFormatUtil() {
    }

    public static String percent(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP).toString().replace('.', ',');
    }

    public static String date(LocalDate value) {
        return value == null ? null : value.format(DATE);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME);
    }

    public static String period(LocalDate start, LocalDate end) {
        String s = date(start);
        String e = date(end);
        if (s != null && e != null) {
            return s + " a " + e;
        }
        if (s != null) {
            return "a partir de " + s;
        }
        if (e != null) {
            return "até " + e;
        }
        return null;
    }

    public static String bandLabel(String bandName) {
        if (bandName == null || bandName.isBlank()) {
            return null;
        }
        try {
            return EthicalComplianceBand.valueOf(bandName.trim().toUpperCase()).getLabel();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String authenticityCode(String prefix, Object... parts) {
        StringBuilder raw = new StringBuilder();
        for (Object part : parts) {
            raw.append(part == null ? "" : part.toString()).append('|');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hash).toUpperCase();
            return prefix + "-" + hex.substring(0, 12);
        } catch (Exception ex) {
            return prefix + "-" + Integer.toHexString(raw.toString().hashCode()).toUpperCase();
        }
    }
}

package com.ethicalsoft.ethicalsoft_complience.domain.i18n;

import java.util.Arrays;
import java.util.Optional;

public enum SupportedLanguage {

    PT_BR("pt-BR", "Português (Brasil)"),
    EN_US("en-US", "English (US)"),
    ES_ES("es-ES", "Español");

    public static final SupportedLanguage DEFAULT = PT_BR;

    private final String code;
    private final String label;

    SupportedLanguage(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static Optional<SupportedLanguage> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(language -> language.code.equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static SupportedLanguage fromCodeOrDefault(String code) {
        return fromCode(code).orElse(DEFAULT);
    }

    public static boolean isDefault(String code) {
        return fromCode(code).map(language -> language == DEFAULT).orElse(false);
    }
}

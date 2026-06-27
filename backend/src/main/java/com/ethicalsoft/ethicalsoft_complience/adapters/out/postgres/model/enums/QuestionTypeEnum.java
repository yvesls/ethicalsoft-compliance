package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums;

import lombok.Getter;

@Getter
public enum QuestionTypeEnum {

    BASE("Base"),
    CUSTOM("Personalizada");

    private final String label;

    QuestionTypeEnum(String label) {
        this.label = label;
    }
}

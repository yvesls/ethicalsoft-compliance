package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums;

import lombok.Getter;

@Getter
public enum QuestionClassificationEnum {

    WHOLE_PROJECT("Projeto inteiro"),
    RECURRING("Recorrente"),
    CURRENT_STAGE("Etapa atual");

    private final String label;

    QuestionClassificationEnum(String label) {
        this.label = label;
    }
}

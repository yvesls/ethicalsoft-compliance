package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums;

import lombok.Getter;

@Getter
public enum QuestionClassificationEnum {

    PROJETO_INTEIRO("Projeto inteiro"),
    BASE_ITERACAO("Base por iteração"),
    ROTATIVA("Rotativa por sprint");

    private final String label;

    QuestionClassificationEnum(String label) {
        this.label = label;
    }
}


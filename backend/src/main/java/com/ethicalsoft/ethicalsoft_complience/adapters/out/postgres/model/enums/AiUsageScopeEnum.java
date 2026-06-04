package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums;

import lombok.Getter;

@Getter
public enum AiUsageScopeEnum {

    NAO_UTILIZA("Não utiliza"),
    REQUISITOS("Utiliza em requisitos"),
    DESIGN_ARQUITETURA("Utiliza em design ou arquitetura"),
    GERACAO_CODIGO("Utiliza em geração de código"),
    TESTES("Utiliza em testes"),
    DOCUMENTACAO("Utiliza em documentação"),
    MANUTENCAO_REFATORACAO("Utiliza em manutenção ou refatoração");

    private final String label;

    AiUsageScopeEnum(String label) {
        this.label = label;
    }
}

package com.ethicalsoft.ethicalsoft_complience.domain.isep;

/** (EXCLUIR DEPOIS)
 * Categorias de domínio de governança para perguntas.
 * Utilizado para calcular sub-índices de conformidade por dimensão,
 * alimentando os indicadores de Dívida Ética/Técnica e alinhamento ESG.
 */
public enum QuestionDomainEnum {

    ETHICS("Ética", "Responsabilidade, privacidade, impacto social e transparência"),
    PROCESS("Processo", "Documentação, rastreabilidade, governança de decisões"),
    QUALITY("Qualidade", "Testes, critérios de aceite, padrões de código"),
    SECURITY("Segurança", "Proteção de dados, vulnerabilidades, controle de acesso"),
    ESG("ESG", "Ambiental, social e governança corporativa"),
    FAIRNESS("Equidade", "Viés algorítmico, acessibilidade, inclusão"),
    AI_GOVERNANCE("Governança do Desenvolvimento Assistido por IA",
            "Rastreabilidade, revisão humana, segurança, privacidade, testes e responsabilidade " +
            "sobre artefatos produzidos com apoio de LLMs");

    private final String label;
    private final String description;

    QuestionDomainEnum(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}


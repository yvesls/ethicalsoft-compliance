package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

@Component
public class AiPromptBuilder {

    private static final String SYSTEM_ROLE_BASE = """
            Você é um analista sênior de conformidade ética em software do sistema EthicalSoft Compliance.

            Você analisa dados do ISEP (Índice Sintético de Ética de Projeto), que mede a conformidade
            ética de projetos de software. Suas análises devem ser:
            - %s
            - Objetivas e acionáveis
            - Baseadas exclusivamente nos dados fornecidos
            - Com foco em riscos éticos, dívida ética e recomendações concretas

            **Fórmula do ISEP:**
            O ISEP é calculado como a média ponderada dos ICP (Índice de Conformidade Pessoal) dos membros.
            Cada ICP é a proporção de respostas "SIM" no questionário do membro.
            O ISEP do projeto é a média ponderada dos ISEPs dos questionários, usando os pesos definidos.

            **Faixas de Conformidade Ética:**
            - A (Excelente): 90,00%% – 100,00%%
            - B (Bom): 75,00%% – 89,99%%
            - C (Regular): 60,00%% – 74,99%%
            - D (Insuficiente): 45,00%% – 59,99%%
            - E (Crítico): 0,00%% – 44,99%%

            **Domínios de Governança:**
            - ETHICS (Ética): Responsabilidade, privacidade, impacto social e transparência
            - PROCESS (Processo): Documentação, rastreabilidade, governança de decisões
            - QUALITY (Qualidade): Testes, critérios de aceite, padrões de código
            - SECURITY (Segurança): Proteção de dados, vulnerabilidades, controle de acesso
            - ESG: Ambiental, social e governança corporativa
            - FAIRNESS (Equidade): Viés algorítmico, acessibilidade, inclusão

            **Dívida Ética (Ethics Debt):** Percentual de não-conformidade no domínio ético.
            Quanto maior, mais decisões antiéticas acumuladas.

            **Dívida Técnica (Tech Debt):** Percentual de não-conformidade nos domínios
            de processo, qualidade e segurança.
            """;

    private String systemRole(String language) {
        String languageInstruction = switch (language == null ? "" : language.trim()) {
            case "en-US" -> "Respond entirely in English (US). Do not include any Portuguese.";
            case "es-ES" -> "Responda íntegramente en español (España). No incluya portugués.";
            default -> "Em português brasileiro (pt-BR)";
        };
        return String.format(SYSTEM_ROLE_BASE, languageInstruction);
    }

    public Prompt buildInsightsPrompt(DashboardSnapshot snapshot, String language) {
        String userData = buildDataContext(snapshot) + "\n\n" + buildJustificationsContext(snapshot);

        String instruction = """
                Analise as justificativas acima considerando o contexto do projeto e seus scores.

                Forneça:
                1. **Padrões Identificados**: Temas recorrentes e correlações entre justificativas e domínios
                2. **Riscos Éticos**: Riscos concretos baseados nos padrões identificados
                3. **Fatores Organizacionais**: Pressões de prazo, custo ou cliente que impactam conformidade
                4. **Recomendações**: Ações específicas e priorizadas para cada risco identificado

                Use emojis para categorizar: 🔴 Crítico, 🟡 Atenção, 🟢 Positivo
                """;

        return new Prompt(List.of(
                new SystemMessage(systemRole(language)),
                new UserMessage(userData + "\n\n" + instruction)
        ));
    }

    public Prompt buildRiskReportPrompt(DashboardSnapshot snapshot, String language) {
        String userData = buildDataContext(snapshot) + "\n\n"
                + buildMembersContext(snapshot) + "\n\n"
                + buildHeatmapContext(snapshot) + "\n\n"
                + buildJustificationsContext(snapshot);

        String instruction = """
                Gere um **Relatório de Risco de Conformidade** completo com as seções:

                ## Resumo Executivo
                Visão geral do estado de conformidade em 3-4 frases.

                ## Alertas Críticos
                Áreas com maior risco ético/técnico. Use dados concretos.

                ## Análise por Domínio
                Avalie cada domínio (ETHICS, PROCESS, FAIRNESS, ESG) com base nos scores.
                Indique quais estão abaixo da média e por quê.

                ## Análise de Equipe
                Identifique outliers (membros com desvio significativo da média).
                Avalie a distribuição por faixas.

                ## Pontos Críticos no Heatmap
                Combinações papel/etapa com pior desempenho.

                ## Recomendações Priorizadas
                Lista numerada de ações concretas, da mais urgente à menos urgente.

                ## Dívida Ética e Técnica
                Avalie os níveis de dívida ética e técnica e suas tendências.
                """;

        return new Prompt(List.of(
                new SystemMessage(systemRole(language)),
                new UserMessage(userData + "\n\n" + instruction)
        ));
    }

    public Prompt buildExplainIsepPrompt(DashboardSnapshot snapshot, String language) {
        String userData = buildDataContext(snapshot) + "\n\n"
                + buildMembersContext(snapshot) + "\n\n"
                + buildHeatmapContext(snapshot);

        String instruction = """
                Explique de forma didática e detalhada **como os resultados ISEP foram calculados**
                e **por que os valores são esses**. Estruture assim:

                ## Como o ISEP é Calculado
                Explique a fórmula: ICP de cada membro → média ponderada → ISEP.
                Use os dados concretos do questionário para ilustrar.

                ## Resultado Geral
                Interprete o ISEP obtido, a faixa de conformidade e o que significa na prática.

                ## Pesos e Média Ponderada
                Explique como os pesos dos questionários afetam o resultado consolidado.

                ## Scores por Domínio — O que cada um significa
                Para cada domínio com score disponível, explique:
                - O que ele mede
                - Qual foi o valor obtido
                - Se está em nível adequado ou preocupante

                ## Dívida Ética vs Dívida Técnica
                Explique a diferença entre os dois indicadores e o que os valores atuais significam.

                ## Classificação por Faixa
                Explique a distribuição dos membros por faixa e o que o desvio padrão indica.

                ## Conclusão
                Resumo com os pontos-chave e o que o gestor deve focar.
                """;

        return new Prompt(List.of(
                new SystemMessage(systemRole(language)),
                new UserMessage(userData + "\n\n" + instruction)
        ));
    }

    public Prompt buildQaPrompt(String question, DashboardSnapshot snapshot, String language) {
        StringBuilder userData = new StringBuilder(buildDataContext(snapshot));

        String members = buildMembersContext(snapshot);
        if (!members.isBlank()) userData.append("\n\n").append(members);

        String heatmap = buildHeatmapContext(snapshot);
        if (!heatmap.isBlank()) userData.append("\n\n").append(heatmap);

        String answerSummary = buildAnswerSummaryContext(snapshot);
        if (!answerSummary.isBlank()) userData.append("\n\n").append(answerSummary);

        String justifications = buildJustificationsContext(snapshot);
        if (!justifications.isBlank()) userData.append("\n\n").append(justifications);

        String instruction = String.format("""
                O usuário fez a seguinte pergunta sobre os dados do dashboard:

                **Pergunta:** %s

                Responda de forma clara, objetiva e baseada exclusivamente nos dados fornecidos.
                Se a pergunta não puder ser respondida com os dados disponíveis, informe isso.
                Use formatação Markdown para melhor legibilidade.
                """, question);

        return new Prompt(List.of(
                new SystemMessage(systemRole(language)),
                new UserMessage(userData + "\n\n" + instruction)
        ));
    }

    private String buildDataContext(DashboardSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Dados do Projeto/Questionário\n\n");
        sb.append("- **Projeto:** ").append(safe(s.getProjectName()))
                .append(" (Tipo: ").append(safe(s.getProjectType())).append(")\n");
        sb.append("- **Questionário:** ").append(safe(s.getQuestionnaireName()));
        if (s.getStageName() != null) sb.append(" | Etapa: ").append(s.getStageName());
        if (s.getIterationName() != null) sb.append(" | Iteração: ").append(s.getIterationName());
        sb.append("\n");
        sb.append("- **ISEP:** ").append(safe(s.getIsepPercent())).append("%");
        sb.append(" | **Faixa:** ").append(safe(s.getBand()));
        if (s.getBandLabel() != null) sb.append(" (").append(s.getBandLabel()).append(")");
        sb.append("\n");
        sb.append("- **Média da equipe:** ").append(safe(s.getTeamAveragePercent())).append("%");
        sb.append(" | **Desvio padrão:** ").append(safe(s.getStandardDeviationPercent())).append("%\n");

        if (s.getBandDistribution() != null && !s.getBandDistribution().isEmpty()) {
            sb.append("- **Distribuição por faixa:** ").append(s.getBandDistribution()).append("\n");
        }

        sb.append("\n### Scores por Domínio\n");
        sb.append("| Domínio | Score (%) |\n|---------|----------|\n");
        appendDomainRow(sb, "Ética (ETHICS)", s.getEthicsScorePercent());
        appendDomainRow(sb, "Processo (PROCESS)", s.getProcessScorePercent());
        appendDomainRow(sb, "Equidade (FAIRNESS)", s.getFairnessScorePercent());
        appendDomainRow(sb, "ESG", s.getEsgScorePercent());
        appendDomainRow(sb, "Dívida Ética", s.getEthicsDebtPercent());
        appendDomainRow(sb, "Dívida Técnica", s.getTechDebtPercent());

        if (s.getWordCloudTopTerms() != null && !s.getWordCloudTopTerms().isEmpty()) {
            sb.append("\n### Termos mais frequentes nas justificativas\n");
            sb.append(String.join(", ", s.getWordCloudTopTerms())).append("\n");
        }

        return sb.toString();
    }

    private String buildMembersContext(DashboardSnapshot s) {
        if (s.getMemberResults() == null || s.getMemberResults().isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("### Membros da Equipe\n");
        sb.append("| Membro | ICP (%) | Faixa |\n|--------|---------|-------|\n");
        for (DashboardSnapshot.MemberSnapshot m : s.getMemberResults()) {
            sb.append("| ").append(safe(m.getRepresentativeName()))
                    .append(" | ").append(safe(m.getIcpPercent()))
                    .append("% | ").append(safe(m.getBand())).append(" |\n");
        }
        return sb.toString();
    }

    private String buildHeatmapContext(DashboardSnapshot s) {
        if (s.getRoleStageHeatmap() == null || s.getRoleStageHeatmap().isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("### Heatmap Papel × Etapa\n");
        sb.append("| Papel | Etapa | IEM (%) | Faixa |\n|-------|-------|---------|-------|\n");
        for (DashboardSnapshot.RoleStageSnapshot rs : s.getRoleStageHeatmap()) {
            sb.append("| ").append(safe(rs.getRoleName()))
                    .append(" | ").append(safe(rs.getStageName()))
                    .append(" | ").append(safe(rs.getIemPercent()))
                    .append("% | ").append(safe(rs.getBand())).append(" |\n");
        }
        return sb.toString();
    }

    private String buildAnswerSummaryContext(DashboardSnapshot s) {
        if (s.getAnswerSummaries() == null || s.getAnswerSummaries().isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("### Respostas por Questão (SIM/NÃO)\n");
        sb.append("| Domínio | Questão | SIM | NÃO | Conformidade (%) |\n");
        sb.append("|---------|---------|-----|-----|------------------|\n");
        for (DashboardSnapshot.AnswerSummarySnapshot a : s.getAnswerSummaries()) {
            sb.append("| ").append(safe(a.getDomain()))
                    .append(" | ").append(truncate(safe(a.getQuestionText()), 60))
                    .append(" | ").append(a.getYesCount())
                    .append(" | ").append(a.getNoCount())
                    .append(" | ").append(safe(a.getCompliancePercent())).append("% |\n");
        }
        return sb.toString();
    }

    private String buildJustificationsContext(DashboardSnapshot s) {
        if (s.getJustifications() == null || s.getJustifications().isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("### Justificativas (amostras sanitizadas)\n");
        sb.append("| Domínio | Pergunta | Resposta | Justificativa |\n");
        sb.append("|---------|----------|----------|---------------|\n");

        int limit = Math.min(s.getJustifications().size(), 50);
        for (int i = 0; i < limit; i++) {
            DashboardSnapshot.JustificationSnapshot j = s.getJustifications().get(i);
            Boolean responseVal = j.getResponse();
            String resposta;
            if (responseVal == null) resposta = "—";
            else if (responseVal) resposta = "SIM";
            else resposta = "NÃO";
            sb.append("| ").append(safe(j.getDomain()))
                    .append(" | ").append(truncate(safe(j.getQuestionText()), 60))
                    .append(" | ").append(resposta)
                    .append(" | ").append(truncate(safe(j.getText()), 80))
                    .append(" |\n");
        }

        if (s.getJustifications().size() > 50) {
            sb.append("\n_(").append(s.getJustifications().size() - 50)
                    .append(" justificativas adicionais omitidas)_\n");
        }

        return sb.toString();
    }

    private void appendDomainRow(StringBuilder sb, String label, Object value) {
        sb.append("| ").append(label).append(" | ").append(safe(value)).append(" |\n");
    }

    private String safe(Object value) {
        return value != null ? value.toString() : "N/D";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "…" : text;
    }
}

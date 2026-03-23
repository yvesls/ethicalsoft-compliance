package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import java.util.List;
import java.util.Map;

/** (EXCLUIR DEPOIS)
 * Nuvem de palavras extraída das justificativas do questionário.
 *
 * Além da frequência global, expõe:
 * - {@code categoryWordFrequency}: palavras agrupadas por domínio de governança
 *   (ETHICS, PROCESS, QUALITY, SECURITY, ESG, FAIRNESS).
 * - {@code topThemeInsights}: insights automáticos por tema, identificando
 *   termos organizacionais (prazo, custo, pressão) associados a domínios sensíveis.
 *
 * Conecta com os discursos de ethical debt / fairness debt / ESG ao mostrar
 * quais fatores organizacionais estão por trás das respostas NÃO.
 */
public record WordCloudDTO(
        Integer questionnaireId,
        Map<String, Long> wordFrequency,
        List<WordEntry> topWords,
        int totalJustifications,
        Map<String, Map<String, Long>> categoryWordFrequency,
        Map<String, String> topThemeInsights
) {
    public WordCloudDTO(Integer questionnaireId,
                        Map<String, Long> wordFrequency,
                        List<WordEntry> topWords,
                        int totalJustifications) {
        this(questionnaireId, wordFrequency, topWords, totalJustifications, null, null);
    }

    public record WordEntry(String word, long frequency) {}
}


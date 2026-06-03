package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireResultRepository;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.WordCloudDTO;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** (EXCLUIR DEPOIS)
 * Gera a nuvem de palavras a partir das justificativas do questionário.
 *
 * Além da frequência global, agrupa as palavras por domínio de governança
 * (ETHICS, PROCESS, QUALITY, SECURITY, ESG, FAIRNESS) usando os metadados
 * das perguntas armazenados no MongoDB.
 *
 * Também gera "insights automáticos" por domínio, identificando termos
 * organizacionais sensíveis (prazo, custo, pressão, cliente) que estão
 * associados a respostas em áreas de risco ético/técnico — conectando
 * diretamente com os conceitos de ethical debt, fairness debt e ESG.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetJustificationWordCloudUseCase {

    private static final int TOP_WORDS_LIMIT = 50;

    private static final Set<String> RISK_INDICATOR_TERMS = Set.of(
            "prazo", "prazos", "custo", "custos", "pressão", "pressao", "cliente",
            "urgente", "urgência", "urgencia", "tempo", "atraso", "atrasado",
            "budget", "orçamento", "orcamento", "meta", "metas", "entrega",
            "deadline", "pressa", "prioridade", "reduzir", "cortar", "economizar"
    );

    private static final Set<String> STOPWORDS = Set.of(
            "a", "ao", "aos", "as", "com", "da", "das", "de", "do", "dos", "e", "em",
            "é", "era", "eu", "foi", "foram", "havia", "isso", "mas", "me", "na", "nas",
            "não", "nem", "no", "nos", "o", "os", "ou", "para", "pela", "pelo", "pelos",
            "pelas", "pois", "por", "que", "se", "ser", "sem", "sobre", "também", "tem",
            "ter", "um", "uma", "uns", "umas", "já", "sua", "seu", "suas", "seus"
    );

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResultRepository questionnaireResultRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final QuestionMetadataRepository questionMetadataRepository;

    @Transactional(readOnly = true)
    public WordCloudDTO execute(Long projectId, Integer questionnaireId) {
        log.info("[word-cloud] Gerando nuvem de palavras questionário={} projeto={}",
                questionnaireId, projectId);

        questionnaireRepository.findByIdAndProjectId(questionnaireId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questionário não encontrado: " + questionnaireId));

        questionnaireResultRepository.findByQuestionnaireId(questionnaireId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resultado ISEP ainda não disponível para o questionário: " + questionnaireId));

        List<QuestionnaireResponse> completedResponses = responseRepository
                .findByProjectIdAndQuestionnaireIdExcludingTemplates(projectId, questionnaireId)
                .stream()
                .filter(r -> QuestionnaireResponseStatus.COMPLETED.equals(r.getStatus()))
                .toList();

        List<String> allJustifications = completedResponses.stream()
                .flatMap(r -> extractJustifications(r).stream())
                .filter(s -> s != null && !s.isBlank())
                .toList();

        log.debug("[word-cloud] {} justificativas coletadas para questionário={}", allJustifications.size(), questionnaireId);

        Map<String, Long> wordFrequency = buildWordFrequency(allJustifications);
        List<WordCloudDTO.WordEntry> topWords = wordFrequency.entrySet().stream()
                .limit(TOP_WORDS_LIMIT)
                .map(e -> new WordCloudDTO.WordEntry(e.getKey(), e.getValue()))
                .toList();

        Set<Long> allQuestionIds = completedResponses.stream()
                .filter(r -> r.getAnswers() != null)
                .flatMap(r -> r.getAnswers().stream())
                .map(QuestionnaireResponse.AnswerDocument::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> questionDomainMap = Collections.emptyMap();
        if (!allQuestionIds.isEmpty()) {
            questionDomainMap = questionMetadataRepository.findByQuestionIdIn(allQuestionIds)
                    .stream()
                    .filter(m -> m.getDomain() != null)
                    .collect(Collectors.toMap(
                            QuestionMetadataDocument::getQuestionId,
                            m -> m.getDomain().name(),
                            (a, b) -> a
                    ));
        }

        Map<String, Map<String, Long>> categoryWordFrequency =
                buildCategoryWordFrequency(completedResponses, questionDomainMap);

        Map<String, String> insights = buildThemeInsights(categoryWordFrequency);

        return new WordCloudDTO(
                questionnaireId,
                wordFrequency,
                topWords,
                allJustifications.size(),
                categoryWordFrequency.isEmpty() ? null : categoryWordFrequency,
                insights.isEmpty() ? null : insights
        );
    }

    private Map<String, Map<String, Long>> buildCategoryWordFrequency(
            List<QuestionnaireResponse> responses,
            Map<Long, String> questionDomainMap) {

        if (questionDomainMap.isEmpty()) return Collections.emptyMap();

        Map<String, List<String>> textsByDomain = new LinkedHashMap<>();

        for (QuestionnaireResponse response : responses) {
            if (response.getAnswers() == null) continue;
            for (QuestionnaireResponse.AnswerDocument answer : response.getAnswers()) {
                if (answer.getQuestionId() == null) continue;
                String domain = questionDomainMap.get(answer.getQuestionId());
                if (domain == null) continue;
                String justText = answer.getJustification() != null
                        ? answer.getJustification().getDescricao()
                        : null;
                if (justText != null && !justText.isBlank()) {
                    textsByDomain.computeIfAbsent(domain, k -> new ArrayList<>()).add(justText);
                }
            }
        }

        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : textsByDomain.entrySet()) {
            Map<String, Long> freq = buildWordFrequency(entry.getValue());
            if (!freq.isEmpty()) {
                Map<String, Long> top = freq.entrySet().stream()
                        .limit(20)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue,
                                (a, b) -> a, LinkedHashMap::new));
                result.put(entry.getKey(), top);
            }
        }
        return result;
    }

    /** (EXCLUIR DEPOIS)
     * Gera insights automáticos por domínio identificando termos de risco organizacional.
     * Ex: "Termos relacionados a 'prazo' e 'custo' apareceram N vezes em justificativas
     * do domínio ETHICS, sugerindo pressão organizacional em áreas éticas sensíveis."
     */
    private Map<String, String> buildThemeInsights(Map<String, Map<String, Long>> categoryWordFrequency) {
        Map<String, String> insights = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Long>> entry : categoryWordFrequency.entrySet()) {
            String domain = entry.getKey();
            Map<String, Long> freq = entry.getValue();

            Map<String, Long> riskTermsFound = freq.entrySet().stream()
                    .filter(e -> RISK_INDICATOR_TERMS.contains(e.getKey()))
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue,
                            (a, b) -> a, LinkedHashMap::new));

            if (riskTermsFound.isEmpty()) continue;

            long totalOccurrences = riskTermsFound.values().stream().mapToLong(Long::longValue).sum();
            List<String> topRiskTerms = riskTermsFound.keySet().stream().limit(3).toList();

            String insight = String.format(
                    "Termos relacionados a '%s' apareceram %d vez(es) em justificativas do domínio %s, " +
                    "sugerindo que fatores organizacionais estão pressionando decisões nessa área.",
                    String.join("', '", topRiskTerms),
                    totalOccurrences,
                    domain
            );
            insights.put(domain, insight);
        }
        return insights;
    }

    private Map<String, Long> buildWordFrequency(List<String> texts) {
        Map<String, Long> freq = texts.stream()
                .flatMap(text -> Arrays.stream(text.toLowerCase().split("[^\\p{L}]+")))
                .map(String::strip)
                .filter(word -> !word.isBlank() && word.length() > 2 && !STOPWORDS.contains(word))
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private List<String> extractJustifications(QuestionnaireResponse response) {
        if (response.getAnswers() == null) return Collections.emptyList();
        List<String> texts = new ArrayList<>();
        for (QuestionnaireResponse.AnswerDocument answer : response.getAnswers()) {
            if (answer.getJustification() != null && answer.getJustification().getDescricao() != null) {
                texts.add(answer.getJustification().getDescricao());
            }
        }
        return texts;
    }
}




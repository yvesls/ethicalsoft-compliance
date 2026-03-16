package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class GetJustificationWordCloudUseCase {

    private static final int TOP_WORDS_LIMIT = 50;

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

        List<String> justifications = responseRepository
                .findByProjectIdAndQuestionnaireId(projectId, questionnaireId)
                .stream()
                .filter(r -> QuestionnaireResponseStatus.COMPLETED.equals(r.getStatus()))
                .flatMap(r -> extractJustifications(r).stream())
                .filter(s -> s != null && !s.isBlank())
                .toList();

        log.debug("[word-cloud] {} justificativas coletadas para questionário={}", justifications.size(), questionnaireId);

        Map<String, Long> wordFrequency = justifications.stream()
                .flatMap(text -> Arrays.stream(text.toLowerCase().split("[^\\p{L}]+")))
                .map(String::strip)
                .filter(word -> !word.isBlank() && word.length() > 2 && !STOPWORDS.contains(word))
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        Map<String, Long> sortedFrequency = wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<WordCloudDTO.WordEntry> topWords = sortedFrequency.entrySet().stream()
                .limit(TOP_WORDS_LIMIT)
                .map(e -> new WordCloudDTO.WordEntry(e.getKey(), e.getValue()))
                .toList();

        return new WordCloudDTO(questionnaireId, sortedFrequency, topWords, justifications.size());
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


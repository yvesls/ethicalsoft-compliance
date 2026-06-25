package com.ethicalsoft.ethicalsoft_complience.application.service.strategy;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IterativeIsepStrategy implements IsepCalculationStrategy {

    private final QuestionMetadataRepository questionMetadataRepository;

    @Override
    public ProjectTypeEnum supportedType() {
        return ProjectTypeEnum.ITERATIVO;
    }

    @Override
    public IsepCalculationResult calculate(Project project,
                                           Questionnaire questionnaire,
                                           List<QuestionnaireResponse> responses,
                                           List<Representative> representatives) {

        log.info("[isep-iterative] Calculando ISEP para questionário id={} projeto id={}",
                questionnaire.getId(), project.getId());

        Map<Long, Representative> repById = representatives.stream()
                .collect(Collectors.toMap(Representative::getId, r -> r, (a, b) -> a));

        Map<Long, Map<Integer, BigDecimal>> memberStageIem = new LinkedHashMap<>();
        Map<Long, BigDecimal> memberIcp = new LinkedHashMap<>();
        List<String> justifications = new ArrayList<>();

        
        for (QuestionnaireResponse response : responses) {
            Long repId = response.getRepresentativeId();
            if (repId == null || response.getAnswers() == null) continue;
            Map<Integer, List<QuestionnaireResponse.AnswerDocument>> allAnswersByStage = groupAnswersByStage(response.getAnswers());
            Map<Integer, BigDecimal> iemByStage = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<QuestionnaireResponse.AnswerDocument>> entry : allAnswersByStage.entrySet()) {
                Integer stageId = entry.getKey();
                List<QuestionnaireResponse.AnswerDocument> stageAnswers = entry.getValue();

                List<QuestionnaireResponse.AnswerDocument> answeredOnly = stageAnswers.stream()
                    .filter(a -> a.getResponse() != null)
                    .toList();

                int totalQuestionsForStage = answeredOnly.size();

                if (totalQuestionsForStage == 0) {
                    iemByStage.put(stageId, BigDecimal.ZERO);
                    continue;
                }
                
                long positiveCount = answeredOnly.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getResponse()))
                    .count();
                
                BigDecimal iem = IsepMath.ratio(positiveCount, totalQuestionsForStage);
                iemByStage.put(stageId, iem);
            }

            memberStageIem.put(repId, iemByStage);

            BigDecimal icp = calculateIcp(questionnaire, iemByStage);
            memberIcp.put(repId, icp);
            log.debug("[isep-iterative] rep={} ICP={}", repId, icp);

            collectJustifications(response.getAnswers(), justifications);
        }

        List<IsepMath.WeightedValue> weightedIcps = memberIcp.entrySet().stream()
                .filter(e -> repById.containsKey(e.getKey()))
                .map(e -> {
                    BigDecimal weight = repById.get(e.getKey()).getWeight();
                    return new IsepMath.WeightedValue(e.getValue(), weight != null ? weight : BigDecimal.ONE);
                })
                .toList();

        BigDecimal isep = IsepMath.weightedAverage(weightedIcps);
        BigDecimal isepPercent = IsepMath.toPercent(isep);
        EthicalComplianceBand band = EthicalComplianceBand.classify(isepPercent);

        BigDecimal simpleAvg = IsepMath.simpleAverage(memberIcp.values());
        BigDecimal stdDev = IsepMath.standardDeviation(memberIcp.values());
        Map<EthicalComplianceBand, Long> distribution = IsepMath.bandDistribution(memberIcp.values());

        log.info("[isep-iterative] ISEP={} ({}) avg={} stdDev={}", isepPercent, band, IsepMath.toPercent(simpleAvg), IsepMath.toPercent(stdDev));

        DomainScores domainScores = calculateDomainScores(responses);

        return new IsepCalculationResult(
                project.getId(),
                questionnaire.getId(),
                memberIcp,
                memberStageIem,
                isep,
                band,
                simpleAvg,
                stdDev,
                distribution,
                justifications,
                domainScores
        );
    }

    private DomainScores calculateDomainScores(List<QuestionnaireResponse> responses) {
        List<QuestionnaireResponse.AnswerDocument> allAnswers = responses.stream()
                .filter(r -> r.getAnswers() != null)
                .flatMap(r -> r.getAnswers().stream())
                .toList();

        Set<Long> questionIds = allAnswers.stream()
                .map(QuestionnaireResponse.AnswerDocument::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (questionIds.isEmpty()) return DomainScores.EMPTY;

        Map<Long, QuestionMetadataDocument> metadataMap = questionMetadataRepository
                .findByQuestionIdIn(questionIds).stream()
                .collect(Collectors.toMap(QuestionMetadataDocument::getQuestionId, m -> m, (a, b) -> a));

        if (metadataMap.isEmpty()) return DomainScores.EMPTY;

        return DomainScoreCalculator.calculate(allAnswers, metadataMap);
    }

    private BigDecimal calculateIcp(Questionnaire questionnaire, Map<Integer, BigDecimal> iemByStage) {
        Map<Integer, BigDecimal> stageWeights = buildStageWeightMap(questionnaire);

        List<IsepMath.WeightedValue> weighted = iemByStage.entrySet().stream()
                .map(e -> {
                    BigDecimal weight = stageWeights.getOrDefault(e.getKey(), BigDecimal.ONE);
                    return new IsepMath.WeightedValue(e.getValue(), weight);
                })
                .toList();

        return IsepMath.weightedAverage(weighted);
    }

    private Map<Integer, BigDecimal> buildStageWeightMap(Questionnaire questionnaire) {
        if (questionnaire.getProject() == null || questionnaire.getProject().getStages() == null) {
            return Collections.emptyMap();
        }
        return questionnaire.getProject().getStages().stream()
                .collect(Collectors.toMap(Stage::getId, s -> s.getWeight() != null ? s.getWeight() : BigDecimal.ONE));
    }

    private Map<Integer, List<QuestionnaireResponse.AnswerDocument>> groupAnswersByStage(
            List<QuestionnaireResponse.AnswerDocument> answers) {

        Map<Integer, List<QuestionnaireResponse.AnswerDocument>> result = new LinkedHashMap<>();
        for (QuestionnaireResponse.AnswerDocument answer : answers) {
            List<Integer> stageIds = answer.getStageIds();
            if (stageIds == null || stageIds.isEmpty()) {
                result.computeIfAbsent(-1, k -> new ArrayList<>()).add(answer);
            } else {
                result.computeIfAbsent(stageIds.get(0), k -> new ArrayList<>()).add(answer);
            }
        }
        return result;
    }

    private void collectJustifications(List<QuestionnaireResponse.AnswerDocument> answers, List<String> dest) {
        for (QuestionnaireResponse.AnswerDocument answer : answers) {
            if (answer.getJustification() != null && answer.getJustification().getDescricao() != null) {
                dest.add(answer.getJustification().getDescricao());
            }
        }
    }
}


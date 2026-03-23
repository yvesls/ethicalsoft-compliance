package com.ethicalsoft.ethicalsoft_complience.application.service.strategy;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
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
public class WaterfallIsepStrategy implements IsepCalculationStrategy {

    private final QuestionMetadataRepository questionMetadataRepository;

    @Override
    public ProjectTypeEnum supportedType() {
        return ProjectTypeEnum.CASCATA;
    }

    @Override
    public IsepCalculationResult calculate(Project project,
                                           Questionnaire questionnaire,
                                           List<QuestionnaireResponse> responses,
                                           List<Representative> representatives) {

        log.info("[isep-waterfall] Calculando ISEP para questionário id={} etapa={} projeto id={}",
                questionnaire.getId(), questionnaire.getName(), project.getId());

        Map<Long, Representative> repById = representatives.stream()
                .collect(Collectors.toMap(Representative::getId, r -> r, (a, b) -> a));

        Map<Long, BigDecimal> memberIcp = new LinkedHashMap<>();
        Map<Long, Map<Integer, BigDecimal>> memberStageIem = new LinkedHashMap<>();
        List<String> justifications = new ArrayList<>();

        Integer stageId = questionnaire.getStage() != null ? questionnaire.getStage().getId() : null;

        for (QuestionnaireResponse response : responses) {
            Long repId = response.getRepresentativeId();
            if (repId == null || response.getAnswers() == null) continue;

            long simCount = response.getAnswers().stream()
                    .filter(a -> Boolean.TRUE.equals(a.getResponse()))
                    .count();
            long total = response.getAnswers().size();
            BigDecimal icp = IsepMath.ratio(simCount, total);
            memberIcp.put(repId, icp);

            if (stageId != null) {
                memberStageIem.put(repId, Map.of(stageId, icp));
            }

            log.debug("[isep-waterfall] rep={} ICP={}", repId, icp);

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

        log.info("[isep-waterfall] ISEP={} ({}) avg={} stdDev={}",
                isepPercent, band, IsepMath.toPercent(simpleAvg), IsepMath.toPercent(stdDev));

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
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        if (questionIds.isEmpty()) return DomainScores.EMPTY;

        Map<Long, QuestionMetadataDocument> metadataMap = questionMetadataRepository
                .findByQuestionIdIn(questionIds).stream()
                .collect(Collectors.toMap(QuestionMetadataDocument::getQuestionId, m -> m, (a, b) -> a));

        if (metadataMap.isEmpty()) return DomainScores.EMPTY;

        return DomainScoreCalculator.calculate(allAnswers, metadataMap);
    }

    private void collectJustifications(List<QuestionnaireResponse.AnswerDocument> answers, List<String> dest) {
        for (QuestionnaireResponse.AnswerDocument answer : answers) {
            if (answer.getJustification() != null && answer.getJustification().getDescricao() != null) {
                dest.add(answer.getJustification().getDescricao());
            }
        }
    }
}


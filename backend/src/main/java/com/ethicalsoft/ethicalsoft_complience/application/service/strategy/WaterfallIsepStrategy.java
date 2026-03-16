package com.ethicalsoft.ethicalsoft_complience.application.service.strategy;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepCalculationResult;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepCalculationStrategy;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WaterfallIsepStrategy implements IsepCalculationStrategy {

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
                justifications
        );
    }

    private void collectJustifications(List<QuestionnaireResponse.AnswerDocument> answers, List<String> dest) {
        for (QuestionnaireResponse.AnswerDocument answer : answers) {
            if (answer.getJustification() != null && answer.getJustification().getDescricao() != null) {
                dest.add(answer.getJustification().getDescricao());
            }
        }
    }
}


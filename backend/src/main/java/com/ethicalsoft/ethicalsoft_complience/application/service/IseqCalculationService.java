package com.ethicalsoft.ethicalsoft_complience.application.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.DomainScoreCalculator;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.DomainScores;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IseqCalculationResult;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IseqCalculationService {

    private static final int NO_STAGE = -1;

    private final QuestionMetadataRepository questionMetadataRepository;

    public IseqCalculationResult calculate(Project project,
                                           Questionnaire questionnaire,
                                           List<QuestionnaireResponse> responses,
                                           List<Representative> representatives) {

        log.info("[iseq] Calculando ISEQ questionário={} projeto={} tipo={}",
                questionnaire.getId(), project.getId(), project.getType());

        Map<Long, Representative> repById = representatives.stream()
                .collect(Collectors.toMap(Representative::getId, r -> r, (a, b) -> a));

        Map<Long, BigDecimal> memberIcp = new LinkedHashMap<>();
        Map<Long, Map<Integer, BigDecimal>> memberStageCompliance = new LinkedHashMap<>();
        List<String> justifications = new ArrayList<>();

        for (QuestionnaireResponse response : responses) {
            Long repId = response.getRepresentativeId();
            if (repId == null || response.getAnswers() == null) {
                continue;
            }

            Set<Long> repRoleIds = roleIdsOf(repById.get(repId));

            List<QuestionnaireResponse.AnswerDocument> eligibleAnswered = response.getAnswers().stream()
                    .filter(a -> a.getResponse() != null)
                    .filter(a -> isEligible(a, repRoleIds))
                    .toList();

            if (eligibleAnswered.isEmpty()) {
                log.debug("[iseq] Representante {} sem perguntas elegíveis respondidas — excluído do ISEQ (sem penalização).", repId);
                collectJustifications(response.getAnswers(), justifications);
                continue;
            }

            memberIcp.put(repId, computeIcp(eligibleAnswered));
            memberStageCompliance.put(repId, computeStageCompliance(eligibleAnswered));
            collectJustifications(response.getAnswers(), justifications);
        }

        BigDecimal iseq = IsepMath.weightedAverage(buildWeightedIcps(memberIcp, repById));
        BigDecimal iseqPercent = IsepMath.toPercent(iseq);
        EthicalComplianceBand band = EthicalComplianceBand.classify(iseqPercent);

        BigDecimal simpleAvg = IsepMath.simpleAverage(memberIcp.values());
        BigDecimal stdDev = IsepMath.standardDeviation(memberIcp.values());
        Map<EthicalComplianceBand, Long> distribution = IsepMath.bandDistribution(memberIcp.values());

        log.info("[iseq] ISEQ={}% faixa={} média={}% desvio={}%",
                iseqPercent, band, IsepMath.toPercent(simpleAvg), IsepMath.toPercent(stdDev));

        DomainScores domainScores = calculateDomainScores(responses);

        return new IseqCalculationResult(
                project.getId(),
                questionnaire.getId(),
                memberIcp,
                memberStageCompliance,
                iseq,
                band,
                simpleAvg,
                stdDev,
                distribution,
                justifications,
                domainScores
        );
    }

    private Set<Long> roleIdsOf(Representative representative) {
        if (representative == null || representative.getRoles() == null) {
            return Set.of();
        }
        return representative.getRoles().stream()
                .map(com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean isEligible(QuestionnaireResponse.AnswerDocument answer, Set<Long> repRoleIds) {
        List<Long> questionRoleIds = answer.getRoleIds();
        if (questionRoleIds == null || questionRoleIds.isEmpty()) {
            return true;
        }
        return questionRoleIds.stream().anyMatch(repRoleIds::contains);
    }

    private BigDecimal computeIcp(List<QuestionnaireResponse.AnswerDocument> eligibleAnswered) {
        int eligible = eligibleAnswered.size();
        if (eligible == 0) {
            return BigDecimal.ZERO;
        }
        long conformes = eligibleAnswered.stream()
                .filter(a -> Boolean.TRUE.equals(a.getResponse()))
                .count();
        return IsepMath.ratio(conformes, eligible);
    }

    private Map<Integer, BigDecimal> computeStageCompliance(List<QuestionnaireResponse.AnswerDocument> eligibleAnswered) {
        Map<Integer, List<QuestionnaireResponse.AnswerDocument>> byStage = new LinkedHashMap<>();
        for (QuestionnaireResponse.AnswerDocument answer : eligibleAnswered) {
            List<Integer> stageIds = answer.getStageIds();
            if (stageIds == null || stageIds.isEmpty()) {
                byStage.computeIfAbsent(NO_STAGE, k -> new ArrayList<>()).add(answer);
            } else {
                for (Integer stageId : stageIds) {
                    byStage.computeIfAbsent(stageId, k -> new ArrayList<>()).add(answer);
                }
            }
        }

        Map<Integer, BigDecimal> stageCompliance = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<QuestionnaireResponse.AnswerDocument>> entry : byStage.entrySet()) {
            List<QuestionnaireResponse.AnswerDocument> stageAnswers = entry.getValue();
            long conformes = stageAnswers.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getResponse()))
                    .count();
            stageCompliance.put(entry.getKey(), IsepMath.ratio(conformes, stageAnswers.size()));
        }
        return stageCompliance;
    }

    private List<IsepMath.WeightedValue> buildWeightedIcps(Map<Long, BigDecimal> memberIcp,
                                                           Map<Long, Representative> repById) {
        return memberIcp.entrySet().stream()
                .filter(e -> repById.containsKey(e.getKey()))
                .map(e -> {
                    BigDecimal weight = repById.get(e.getKey()).getWeight();
                    return new IsepMath.WeightedValue(e.getValue(), weight != null ? weight : BigDecimal.ONE);
                })
                .toList();
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

        if (questionIds.isEmpty()) {
            return DomainScores.EMPTY;
        }

        Map<Long, QuestionMetadataDocument> metadataMap = questionMetadataRepository
                .findByQuestionIdIn(questionIds).stream()
                .collect(Collectors.toMap(QuestionMetadataDocument::getQuestionId, m -> m, (a, b) -> a));

        if (metadataMap.isEmpty()) {
            return DomainScores.EMPTY;
        }

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

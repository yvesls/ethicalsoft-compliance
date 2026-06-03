package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.QuestionAnalysis;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.QuestionDataType;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.QuestionnaireIsepDashboardDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.RoleStageComplianceDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.WordCloudDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardSnapshotAssembler {

    private final QuestionnaireResponseRepository responseRepository;
    private final QuestionMetadataRepository questionMetadataRepository;

    @Value("${app.ai.max-justifications-per-request:50}")
    private int maxJustifications;

    public DashboardSnapshot assemble(
            Long projectId,
            String projectName,
            String projectType,
            QuestionnaireIsepDashboardDTO dashboard,
            List<RoleStageComplianceDTO> roleStageList,
            WordCloudDTO wordCloud) {

        List<DashboardSnapshot.MemberSnapshot> members = null;
        if (dashboard.memberResults() != null) {
            members = dashboard.memberResults().stream()
                    .map(m -> DashboardSnapshot.MemberSnapshot.builder()
                            .representativeId(m.representativeId())
                            .representativeName(m.representativeName())
                            .icpPercent(m.icpPercent())
                            .band(m.band())
                            .build())
                    .toList();
        }

        List<DashboardSnapshot.RoleStageSnapshot> heatmap = new ArrayList<>();
        if (roleStageList != null) {
            for (RoleStageComplianceDTO rs : roleStageList) {
                if (rs.iemByStage() != null) {
                    for (RoleStageComplianceDTO.StageIemSummary stage : rs.iemByStage().values()) {
                        heatmap.add(DashboardSnapshot.RoleStageSnapshot.builder()
                                .roleName(rs.roleName())
                                .stageName(stage.stageName())
                                .iemPercent(stage.iemPercent())
                                .band(stage.band())
                                .build());
                    }
                }
            }
        }

        List<String> topTerms = null;
        if (wordCloud != null && wordCloud.topWords() != null) {
            topTerms = wordCloud.topWords().stream()
                    .limit(20)
                    .map(WordCloudDTO.WordEntry::word)
                    .toList();
        }

        List<DashboardSnapshot.JustificationSnapshot> justifications =
                loadJustifications(projectId, dashboard.questionnaireId());

        String bandLabel = null;
        if (dashboard.band() != null) {
            try {
                bandLabel = EthicalComplianceBand.valueOf(dashboard.band()).getLabel();
            } catch (IllegalArgumentException ignored) {}
        }

        return DashboardSnapshot.builder()
                .projectId(projectId)
                .projectName(projectName)
                .projectType(projectType)
                .questionnaireId(dashboard.questionnaireId())
                .questionnaireName(dashboard.questionnaireName())
                .stageName(dashboard.stageName())
                .iterationName(dashboard.iterationName())
                .isepPercent(dashboard.isepPercent())
                .band(dashboard.band())
                .bandLabel(bandLabel)
                .teamAveragePercent(dashboard.teamSimpleAveragePercent())
                .standardDeviationPercent(dashboard.teamStandardDeviationPercent())
                .bandDistribution(dashboard.bandDistribution())
                .ethicsScorePercent(dashboard.ethicsScorePercent())
                .processScorePercent(dashboard.processScorePercent())
                .fairnessScorePercent(dashboard.fairnessScorePercent())
                .esgScorePercent(dashboard.esgScorePercent())
                .ethicsDebtPercent(dashboard.ethicsDebtPercent())
                .techDebtPercent(dashboard.techDebtPercent())
                .memberResults(members)
                .roleStageHeatmap(heatmap.isEmpty() ? null : heatmap)
                .justifications(justifications)
                .wordCloudTopTerms(topTerms)
                .build();
    }

    public DashboardSnapshot assembleContextual(
            Long projectId,
            String projectName,
            String projectType,
            QuestionnaireIsepDashboardDTO dashboard,
            List<RoleStageComplianceDTO> roleStageList,
            WordCloudDTO wordCloud,
            QuestionAnalysis analysis) {

        List<DashboardSnapshot.MemberSnapshot> members = null;
        if (dashboard.memberResults() != null) {
            members = dashboard.memberResults().stream()
                    .map(m -> DashboardSnapshot.MemberSnapshot.builder()
                            .representativeId(m.representativeId())
                            .representativeName(m.representativeName())
                            .icpPercent(m.icpPercent())
                            .band(m.band())
                            .build())
                    .toList();
        }

        List<DashboardSnapshot.RoleStageSnapshot> heatmap = null;
        if (analysis.includes(QuestionDataType.ROLE_STAGE) && roleStageList != null) {
            heatmap = new ArrayList<>();
            for (RoleStageComplianceDTO rs : roleStageList) {
                if (rs.iemByStage() != null) {
                    for (RoleStageComplianceDTO.StageIemSummary stage : rs.iemByStage().values()) {
                        heatmap.add(DashboardSnapshot.RoleStageSnapshot.builder()
                                .roleName(rs.roleName())
                                .stageName(stage.stageName())
                                .iemPercent(stage.iemPercent())
                                .band(stage.band())
                                .build());
                    }
                }
            }
            if (heatmap.isEmpty()) heatmap = null;
        }

        List<String> topTerms = null;
        if (analysis.includes(QuestionDataType.WORD_CLOUD) && wordCloud != null
                && wordCloud.topWords() != null) {
            topTerms = wordCloud.topWords().stream()
                    .limit(20)
                    .map(WordCloudDTO.WordEntry::word)
                    .toList();
        }

        List<DashboardSnapshot.JustificationSnapshot> justifications = null;
        if (analysis.includes(QuestionDataType.JUSTIFICATIONS)) {
            justifications = loadJustifications(projectId, dashboard.questionnaireId(),
                    analysis.domainFilter());
        }

        List<DashboardSnapshot.AnswerSummarySnapshot> answerSummaries = null;
        if (analysis.includes(QuestionDataType.ANSWER_SUMMARY)) {
            answerSummaries = loadAnswerSummaries(projectId, dashboard.questionnaireId());
        }

        String bandLabel = null;
        if (dashboard.band() != null) {
            try {
                bandLabel = EthicalComplianceBand.valueOf(dashboard.band()).getLabel();
            } catch (IllegalArgumentException ignored) {
            }
        }

        return DashboardSnapshot.builder()
                .projectId(projectId)
                .projectName(projectName)
                .projectType(projectType)
                .questionnaireId(dashboard.questionnaireId())
                .questionnaireName(dashboard.questionnaireName())
                .stageName(dashboard.stageName())
                .iterationName(dashboard.iterationName())
                .isepPercent(dashboard.isepPercent())
                .band(dashboard.band())
                .bandLabel(bandLabel)
                .teamAveragePercent(dashboard.teamSimpleAveragePercent())
                .standardDeviationPercent(dashboard.teamStandardDeviationPercent())
                .bandDistribution(dashboard.bandDistribution())
                .ethicsScorePercent(dashboard.ethicsScorePercent())
                .processScorePercent(dashboard.processScorePercent())
                .fairnessScorePercent(dashboard.fairnessScorePercent())
                .esgScorePercent(dashboard.esgScorePercent())
                .ethicsDebtPercent(dashboard.ethicsDebtPercent())
                .techDebtPercent(dashboard.techDebtPercent())
                .memberResults(members)
                .roleStageHeatmap(heatmap)
                .justifications(justifications)
                .wordCloudTopTerms(topTerms)
                .answerSummaries(answerSummaries)
                .build();
    }

    private List<DashboardSnapshot.AnswerSummarySnapshot> loadAnswerSummaries(
            Long projectId, Integer questionnaireId) {

        List<QuestionnaireResponse> responses = responseRepository
                .findByProjectIdAndQuestionnaireIdExcludingTemplates(projectId, questionnaireId)
                .stream()
                .filter(r -> QuestionnaireResponseStatus.COMPLETED.equals(r.getStatus()))
                .toList();

        Set<Long> allQuestionIds = responses.stream()
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

        Map<Long, long[]> counts = new LinkedHashMap<>();
        Map<Long, String> qTextMap = new HashMap<>();
        Map<Long, String> finalDomainMap = questionDomainMap;

        for (QuestionnaireResponse response : responses) {
            if (response.getAnswers() == null) continue;
            for (QuestionnaireResponse.AnswerDocument answer : response.getAnswers()) {
                Long qId = answer.getQuestionId();
                if (qId == null || answer.getResponse() == null) continue;
                counts.computeIfAbsent(qId, k -> new long[2]);
                if (Boolean.TRUE.equals(answer.getResponse())) {
                    counts.get(qId)[0]++;
                } else {
                    counts.get(qId)[1]++;
                }
                qTextMap.putIfAbsent(qId, answer.getQuestionText());
            }
        }

        return counts.entrySet().stream()
                .map(e -> {
                    long yes = e.getValue()[0];
                    long no = e.getValue()[1];
                    long total = yes + no;
                    BigDecimal compliance = total > 0
                            ? BigDecimal.valueOf(yes * 100)
                                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return DashboardSnapshot.AnswerSummarySnapshot.builder()
                            .questionText(qTextMap.get(e.getKey()))
                            .domain(finalDomainMap.getOrDefault(e.getKey(), "N/D"))
                            .yesCount(yes)
                            .noCount(no)
                            .compliancePercent(compliance)
                            .build();
                })
                .sorted(Comparator.comparing(DashboardSnapshot.AnswerSummarySnapshot::getCompliancePercent))
                .toList();
    }

    private List<DashboardSnapshot.JustificationSnapshot> loadJustifications(
            Long projectId, Integer questionnaireId) {
        return loadJustifications(projectId, questionnaireId, null);
    }

    private List<DashboardSnapshot.JustificationSnapshot> loadJustifications(
            Long projectId, Integer questionnaireId, String domainFilter) {

        List<QuestionnaireResponse> responses = responseRepository
                .findByProjectIdAndQuestionnaireIdExcludingTemplates(projectId, questionnaireId)
                .stream()
                .filter(r -> QuestionnaireResponseStatus.COMPLETED.equals(r.getStatus()))
                .toList();

        Set<Long> allQuestionIds = responses.stream()
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

        List<DashboardSnapshot.JustificationSnapshot> justifications = new ArrayList<>();
        Map<Long, String> finalMap = questionDomainMap;

        for (QuestionnaireResponse response : responses) {
            if (response.getAnswers() == null) continue;
            for (QuestionnaireResponse.AnswerDocument answer : response.getAnswers()) {
                if (answer.getJustification() != null
                        && answer.getJustification().getDescricao() != null
                        && !answer.getJustification().getDescricao().isBlank()) {

                    String domain = answer.getQuestionId() != null
                            ? finalMap.getOrDefault(answer.getQuestionId(), "N/D")
                            : "N/D";

                    justifications.add(DashboardSnapshot.JustificationSnapshot.builder()
                            .domain(domain)
                            .questionText(answer.getQuestionText())
                            .text(answer.getJustification().getDescricao())
                            .response(answer.getResponse())
                            .build());
                }
            }
        }

        List<DashboardSnapshot.JustificationSnapshot> result = justifications;
        if (domainFilter != null && !domainFilter.isBlank()) {
            result = result.stream()
                    .filter(j -> domainFilter.equalsIgnoreCase(j.getDomain()))
                    .toList();
        }

        if (result.size() > maxJustifications) {
            log.debug("[snapshot-assembler] Truncando justificativas de {} para {}",
                    result.size(), maxJustifications);
            return result.subList(0, maxJustifications);
        }

        return result;
    }
}


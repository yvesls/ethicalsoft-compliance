package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DashboardSnapshot {
    private Long projectId;
    private String projectName;
    private String projectType;

    private Integer questionnaireId;
    private String questionnaireName;
    private String stageName;
    private String iterationName;

    private BigDecimal isepPercent;
    private String band;
    private String bandLabel;
    private BigDecimal teamAveragePercent;
    private BigDecimal standardDeviationPercent;

    private Map<String, Long> bandDistribution;

    private BigDecimal ethicsScorePercent;
    private BigDecimal processScorePercent;
    private BigDecimal fairnessScorePercent;
    private BigDecimal esgScorePercent;
    private BigDecimal ethicsDebtPercent;
    private BigDecimal techDebtPercent;

    private List<MemberSnapshot> memberResults;

    private List<RoleStageSnapshot> roleStageHeatmap;

    private List<JustificationSnapshot> justifications;

    private List<String> wordCloudTopTerms;

    private List<AnswerSummarySnapshot> answerSummaries;

    @Getter
    @Builder
    public static class MemberSnapshot {
        private Long representativeId;
        private String representativeName;
        private BigDecimal icpPercent;
        private String band;
    }

    @Getter
    @Builder
    public static class RoleStageSnapshot {
        private String roleName;
        private String stageName;
        private BigDecimal iemPercent;
        private String band;
    }

    @Getter
    @Builder
    public static class JustificationSnapshot {
        private String domain;
        private String questionText;
        private String text;
        private Boolean response;
    }

    @Getter
    @Builder
    public static class AnswerSummarySnapshot {
        private String questionText;
        private String domain;
        private long yesCount;
        private long noCount;
        private BigDecimal compliancePercent;
    }
}


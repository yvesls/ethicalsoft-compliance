package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_isep_result")
public class ProjectIsepResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;

    @Column(name = "isep", nullable = false, precision = 7, scale = 4)
    private BigDecimal isep;

    @Column(name = "band", nullable = false, length = 1)
    private String band;

    @Column(name = "questionnaire_count", nullable = false)
    private Integer questionnaireCount;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "closed_by", length = 255)
    private String closedBy;

    @Column(name = "team_simple_avg", precision = 7, scale = 4)
    private BigDecimal teamSimpleAverage;

    @Column(name = "team_std_dev", precision = 7, scale = 4)
    private BigDecimal teamStandardDeviation;

    @Column(name = "ethics_score", precision = 7, scale = 4)
    private BigDecimal ethicsScore;

    @Column(name = "process_score", precision = 7, scale = 4)
    private BigDecimal processScore;

    @Column(name = "fairness_score", precision = 7, scale = 4)
    private BigDecimal fairnessScore;

    @Column(name = "esg_score", precision = 7, scale = 4)
    private BigDecimal esgScore;

    @Column(name = "ethics_debt_score", precision = 7, scale = 4)
    private BigDecimal ethicsDebtScore;

    @Column(name = "tech_debt_score", precision = 7, scale = 4)
    private BigDecimal techDebtScore;
}


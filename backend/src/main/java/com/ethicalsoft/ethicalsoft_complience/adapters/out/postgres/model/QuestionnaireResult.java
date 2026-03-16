package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "questionnaire_result")
public class QuestionnaireResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "questionnaire_id", nullable = false)
    private Integer questionnaireId;

    @Column(name = "isep", nullable = false, precision = 7, scale = 4)
    private BigDecimal isep;

    @Column(name = "band", nullable = false, length = 1)
    private String band;

    @Column(name = "team_simple_avg", nullable = false, precision = 7, scale = 4)
    private BigDecimal teamSimpleAverage;

    @Column(name = "team_std_dev", nullable = false, precision = 7, scale = 4)
    private BigDecimal teamStandardDeviation;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberComplianceResult> memberResults = new ArrayList<>();

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberStageComplianceResult> stageResults = new ArrayList<>();
}


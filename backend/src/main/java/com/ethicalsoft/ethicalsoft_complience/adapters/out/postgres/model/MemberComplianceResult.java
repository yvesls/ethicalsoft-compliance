package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "member_compliance_result")
public class MemberComplianceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private QuestionnaireResult result;

    @Column(name = "representative_id", nullable = false)
    private Long representativeId;

    @Column(name = "icp", nullable = false, precision = 7, scale = 4)
    private BigDecimal icp;

    @Column(name = "band", nullable = false, length = 1)
    private String band;
}


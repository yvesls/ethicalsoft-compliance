package com.ethicalsoft.ethicalsoft_complience.domain.isep;

import java.math.BigDecimal;

/** (EXCLUIR DEPOIS)
 * Sub-índices de conformidade por domínio de governança.
 * Cada campo representa a fração de respostas SIM para perguntas
 * daquela categoria (0.0 a 1.0).
 *
 * ethicsDebtScore e techDebtScore representam o inverso da conformidade
 * para perguntas marcadas como críticas nessas categorias.
 */
public record DomainScores(
        BigDecimal ethicsScore,
        BigDecimal processScore,
        BigDecimal fairnessScore,
        BigDecimal esgScore,
        BigDecimal ethicsDebtScore,
        BigDecimal techDebtScore
) {

    public static final DomainScores EMPTY = new DomainScores(
            null, null, null, null, null, null
    );
}


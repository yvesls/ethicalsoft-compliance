package com.ethicalsoft.ethicalsoft_complience.domain.isep;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse.AnswerDocument;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** (EXCLUIR DEPOIS)
 * Calcula sub-índices de conformidade por domínio de governança.
 * Utiliza os metadados das perguntas (QuestionMetadataDocument) para
 * agrupar respostas por domínio e calcular:
 * - ethicsScore, processScore, fairnessScore, esgScore (fração SIM/total por domínio)
 * - ethicsDebtScore (fração NÃO em perguntas críticas ETHICS/FAIRNESS)
 * - techDebtScore (fração NÃO em perguntas críticas PROCESS/QUALITY)
 */
public final class DomainScoreCalculator {

    private DomainScoreCalculator() {}

    /** (EXCLUIR DEPOIS)
     * Calcula DomainScores agregados para todas as respostas de um questionário.
     *
     * @param allAnswers     lista achatada de todas as respostas de todos os membros
     * @param metadataByQId  mapa questionId → QuestionMetadataDocument
     * @return DomainScores com os sub-índices calculados
     */
    public static DomainScores calculate(List<AnswerDocument> allAnswers,
                                         Map<Long, QuestionMetadataDocument> metadataByQId) {
        if (allAnswers == null || allAnswers.isEmpty() || metadataByQId == null || metadataByQId.isEmpty()) {
            return DomainScores.EMPTY;
        }

        Map<QuestionDomainEnum, List<AnswerDocument>> byDomain = new EnumMap<>(QuestionDomainEnum.class);
        List<AnswerDocument> criticalEthics = new ArrayList<>();
        List<AnswerDocument> criticalTech = new ArrayList<>();

        for (AnswerDocument answer : allAnswers.stream()
                .filter(answer -> answer.getQuestionId() != null)
                .filter(answer -> {
                    QuestionMetadataDocument meta = metadataByQId.get(answer.getQuestionId());
                    return meta != null && meta.getDomain() != null;
                })
                .toList()) {
            QuestionMetadataDocument meta = metadataByQId.get(answer.getQuestionId());
            byDomain.computeIfAbsent(meta.getDomain(), k -> new ArrayList<>()).add(answer);

            if (meta.isCritical()) {
                if (meta.getDomain() == QuestionDomainEnum.ETHICS
                        || meta.getDomain() == QuestionDomainEnum.FAIRNESS
                        || meta.getDomain() == QuestionDomainEnum.ESG) {
                    criticalEthics.add(answer);
                }
                if (meta.getDomain() == QuestionDomainEnum.PROCESS
                        || meta.getDomain() == QuestionDomainEnum.QUALITY
                        || meta.getDomain() == QuestionDomainEnum.SECURITY) {
                    criticalTech.add(answer);
                }
            }
        }

        BigDecimal ethicsScore = domainRatio(byDomain.get(QuestionDomainEnum.ETHICS));
        BigDecimal processScore = domainRatio(byDomain.get(QuestionDomainEnum.PROCESS));
        BigDecimal fairnessScore = domainRatio(byDomain.get(QuestionDomainEnum.FAIRNESS));
        BigDecimal esgScore = domainRatio(byDomain.get(QuestionDomainEnum.ESG));

        BigDecimal ethicsDebtScore = debtRatio(criticalEthics);
        BigDecimal techDebtScore = debtRatio(criticalTech);

        return new DomainScores(ethicsScore, processScore, fairnessScore, esgScore, ethicsDebtScore, techDebtScore);
    }

    private static BigDecimal domainRatio(List<AnswerDocument> answers) {
        if (answers == null || answers.isEmpty()) return null;
        long sim = answers.stream().filter(a -> Boolean.TRUE.equals(a.getResponse())).count();
        return IsepMath.ratio(sim, answers.size());
    }

    private static BigDecimal debtRatio(List<AnswerDocument> criticalAnswers) {
        if (criticalAnswers == null || criticalAnswers.isEmpty()) return null;
        long nao = criticalAnswers.stream().filter(a -> !Boolean.TRUE.equals(a.getResponse())).count();
        return IsepMath.ratio(nao, criticalAnswers.size());
    }
}


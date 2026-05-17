package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sanitizador LGPD para dados enviados ao LLM.
 *
 * <p>Remove dados pessoais sensíveis (e-mail, CPF, URLs) das justificativas
 * antes do envio à IA em nuvem.</p>
 *
 * <p><b>Nota:</b> Os nomes dos representantes são MANTIDOS pois o consentimento
 * será gerenciado em feature separada. O representativeId é removido.</p>
 */
@Component
@Slf4j
public class AiDataSanitizer {

    public DashboardSnapshot sanitize(DashboardSnapshot raw) {
        log.debug("[ai-sanitizer] Sanitizando snapshot para projeto={}", raw.getProjectId());

        List<DashboardSnapshot.MemberSnapshot> sanitizedMembers = null;
        if (raw.getMemberResults() != null) {
            sanitizedMembers = raw.getMemberResults().stream()
                    .map(m -> DashboardSnapshot.MemberSnapshot.builder()
                            .representativeId(null)
                            .representativeName(m.getRepresentativeName())
                            .icpPercent(m.getIcpPercent())
                            .band(m.getBand())
                            .build())
                    .toList();
        }

        List<DashboardSnapshot.JustificationSnapshot> sanitizedJustifications = null;
        if (raw.getJustifications() != null) {
            sanitizedJustifications = raw.getJustifications().stream()
                    .map(j -> DashboardSnapshot.JustificationSnapshot.builder()
                            .domain(j.getDomain())
                            .questionText(j.getQuestionText())
                            .text(sanitizeText(j.getText()))
                            .response(j.getResponse())
                            .build())
                    .toList();
        }

        return DashboardSnapshot.builder()
                .projectId(raw.getProjectId())
                .projectName(raw.getProjectName())
                .projectType(raw.getProjectType())
                .questionnaireId(raw.getQuestionnaireId())
                .questionnaireName(raw.getQuestionnaireName())
                .stageName(raw.getStageName())
                .iterationName(raw.getIterationName())
                .isepPercent(raw.getIsepPercent())
                .band(raw.getBand())
                .bandLabel(raw.getBandLabel())
                .teamAveragePercent(raw.getTeamAveragePercent())
                .standardDeviationPercent(raw.getStandardDeviationPercent())
                .bandDistribution(raw.getBandDistribution())
                .ethicsScorePercent(raw.getEthicsScorePercent())
                .processScorePercent(raw.getProcessScorePercent())
                .fairnessScorePercent(raw.getFairnessScorePercent())
                .esgScorePercent(raw.getEsgScorePercent())
                .ethicsDebtPercent(raw.getEthicsDebtPercent())
                .techDebtPercent(raw.getTechDebtPercent())
                .memberResults(sanitizedMembers)
                .roleStageHeatmap(raw.getRoleStageHeatmap())
                .justifications(sanitizedJustifications)
                .wordCloudTopTerms(raw.getWordCloudTopTerms())
                .build();
    }

    private String sanitizeText(String text) {
        if (text == null) return null;
        String result = text;
        result = removeEmails(result);
        result = removeCpfPatterns(result);
        result = removeUrls(result);
        result = removePhoneNumbers(result);
        return result;
    }

    private String removeEmails(String text) {
        return text.replaceAll("[\\w.-]+@[\\w.-]+\\.\\w+", "[EMAIL_REMOVIDO]");
    }

    private String removeCpfPatterns(String text) {
        return text.replaceAll("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", "[CPF_REMOVIDO]");
    }

    private String removeUrls(String text) {
        return text.replaceAll("https?://\\S+", "[URL_REMOVIDA]");
    }

    private String removePhoneNumbers(String text) {
        return text.replaceAll("\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}", "[TELEFONE_REMOVIDO]");
    }
}



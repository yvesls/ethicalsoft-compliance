package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportIsepCsvUseCase {

    private static final String DELIMITER = ";";
    private static final String LINE_SEP = "\n";

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final RepresentativeRepository representativeRepository;
    private final StageRepository stageRepository;
    private final IsepResultQueryPort isepResultQueryPort;

    @Transactional(readOnly = true)
    public String executeAll(Long projectId, boolean anonymize) {
        log.info("[isep-csv-export] Exportando todos os dados projeto={} anonymize={}", projectId, anonymize);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + projectId));

        List<QuestionnaireResult> results = isepResultQueryPort.findByProjectId(projectId);
        if (results.isEmpty()) {
            return buildHeader(List.of()) + LINE_SEP;
        }

        List<Stage> stages = stageRepository.findByProjectId(projectId);
        List<Representative> reps = representativeRepository.findByProjectId(projectId);

        Map<Long, Representative> repMap = reps.stream()
                .collect(Collectors.toMap(Representative::getId, r -> r));

        List<String> stageNames = stages.stream()
                .sorted(Comparator.comparing(Stage::getId))
                .map(Stage::getName)
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append(buildHeader(stageNames));

        Map<Integer, Questionnaire> qMap = questionnaireRepository.findByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(Questionnaire::getId, q -> q));

        for (QuestionnaireResult result : results) {
            Questionnaire q = qMap.get(result.getQuestionnaireId());
            String questionnaireName = q != null ? q.getName() : "Q" + result.getQuestionnaireId();
            String iterationOrStage = q != null && q.getIteration() != null
                    ? q.getIteration()
                    : (q != null && q.getStage() != null ? q.getStage().getName() : questionnaireName);

            Map<Long, Map<Integer, BigDecimal>> iemByRepByStage = result.getStageResults().stream()
                    .collect(Collectors.groupingBy(
                            MemberStageComplianceResult::getRepresentativeId,
                            Collectors.toMap(
                                    MemberStageComplianceResult::getStageId,
                                    r -> r.getIem() != null ? r.getIem() : BigDecimal.ZERO
                            )
                    ));

            for (MemberComplianceResult mcr : result.getMemberResults()) {
                Representative rep = repMap.get(mcr.getRepresentativeId());
                String memberId = anonymize
                        ? "MBR-" + Math.abs(mcr.getRepresentativeId().hashCode() % 10000)
                        : String.valueOf(mcr.getRepresentativeId());
                String memberName = anonymize || rep == null || rep.getUser() == null
                        ? ""
                        : rep.getUser().getFirstName() + " " + rep.getUser().getLastName();

                StringBuilder row = new StringBuilder();
                row.append(escapeCsv(project.getName())).append(DELIMITER);
                row.append(escapeCsv(questionnaireName)).append(DELIMITER);
                row.append(escapeCsv(iterationOrStage)).append(DELIMITER);
                row.append(escapeCsv(result.getCalculatedAt() != null ? result.getCalculatedAt().toString() : "")).append(DELIMITER);
                row.append(escapeCsv(memberId)).append(DELIMITER);
                row.append(escapeCsv(memberName)).append(DELIMITER);
                row.append(IsepMath.toPercent(mcr.getIcp()).toPlainString()).append(DELIMITER);
                row.append(escapeCsv(mcr.getBand())).append(DELIMITER);
                row.append(IsepMath.toPercent(result.getIseq()).toPlainString()).append(DELIMITER);
                row.append(escapeCsv(result.getBand())).append(DELIMITER);
                row.append(percentOrEmpty(result.getEthicsScore())).append(DELIMITER);
                row.append(percentOrEmpty(result.getProcessScore())).append(DELIMITER);
                row.append(percentOrEmpty(result.getFairnessScore())).append(DELIMITER);
                row.append(percentOrEmpty(result.getEsgScore())).append(DELIMITER);
                row.append(percentOrEmpty(result.getEthicsDebtScore())).append(DELIMITER);
                row.append(percentOrEmpty(result.getTechDebtScore()));

                Map<Integer, BigDecimal> repIem = iemByRepByStage.getOrDefault(mcr.getRepresentativeId(), Map.of());
                for (Stage stage : stages.stream().sorted(Comparator.comparing(Stage::getId)).toList()) {
                    row.append(DELIMITER);
                    BigDecimal iem = repIem.get(stage.getId());
                    row.append(iem != null ? IsepMath.toPercent(iem).toPlainString() : "");
                }

                csv.append(LINE_SEP).append(row);
            }
        }

        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String execute(Long projectId, Integer questionnaireId, boolean anonymize) {
        log.info("[isep-csv-export] Exportando questionário={} projeto={} anonymize={}",
                questionnaireId, projectId, anonymize);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + projectId));

        Questionnaire questionnaire = questionnaireRepository.findByIdAndProjectId(questionnaireId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionário não encontrado: " + questionnaireId));

        QuestionnaireResult result = isepResultQueryPort.findByQuestionnaireId(questionnaireId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resultado ISEP ainda não disponível para o questionário: " + questionnaireId));

        List<Stage> stages = stageRepository.findByProjectId(projectId)
                .stream()
                .sorted(Comparator.comparing(Stage::getId))
                .toList();

        List<Representative> reps = representativeRepository.findByProjectId(projectId);
        Map<Long, Representative> repMap = reps.stream()
                .collect(Collectors.toMap(Representative::getId, r -> r));

        List<String> stageNames = stages.stream().map(Stage::getName).toList();

        Map<Long, Map<Integer, BigDecimal>> iemByRepByStage = result.getStageResults().stream()
                .collect(Collectors.groupingBy(
                        MemberStageComplianceResult::getRepresentativeId,
                        Collectors.toMap(
                                MemberStageComplianceResult::getStageId,
                                r -> r.getIem() != null ? r.getIem() : BigDecimal.ZERO
                        )
                ));

        String questionnaireName = questionnaire.getName();
        String iterationOrStage = questionnaire.getIteration() != null
                ? questionnaire.getIteration()
                : (questionnaire.getStage() != null ? questionnaire.getStage().getName() : questionnaireName);

        StringBuilder csv = new StringBuilder();
        csv.append(buildHeader(stageNames));

        for (MemberComplianceResult mcr : result.getMemberResults()) {
            Representative rep = repMap.get(mcr.getRepresentativeId());
            String memberId = anonymize
                    ? "MBR-" + Math.abs(mcr.getRepresentativeId().hashCode() % 10000)
                    : String.valueOf(mcr.getRepresentativeId());
            String memberName = anonymize || rep == null || rep.getUser() == null
                    ? ""
                    : rep.getUser().getFirstName() + " " + rep.getUser().getLastName();

            StringBuilder row = new StringBuilder();
            row.append(escapeCsv(project.getName())).append(DELIMITER);
            row.append(escapeCsv(questionnaireName)).append(DELIMITER);
            row.append(escapeCsv(iterationOrStage)).append(DELIMITER);
            row.append(escapeCsv(result.getCalculatedAt() != null ? result.getCalculatedAt().toString() : "")).append(DELIMITER);
            row.append(escapeCsv(memberId)).append(DELIMITER);
            row.append(escapeCsv(memberName)).append(DELIMITER);
            row.append(IsepMath.toPercent(mcr.getIcp()).toPlainString()).append(DELIMITER);
            row.append(escapeCsv(mcr.getBand())).append(DELIMITER);
            row.append(IsepMath.toPercent(result.getIseq()).toPlainString()).append(DELIMITER);
            row.append(escapeCsv(result.getBand())).append(DELIMITER);
            row.append(percentOrEmpty(result.getEthicsScore())).append(DELIMITER);
            row.append(percentOrEmpty(result.getProcessScore())).append(DELIMITER);
            row.append(percentOrEmpty(result.getFairnessScore())).append(DELIMITER);
            row.append(percentOrEmpty(result.getEsgScore())).append(DELIMITER);
            row.append(percentOrEmpty(result.getEthicsDebtScore())).append(DELIMITER);
            row.append(percentOrEmpty(result.getTechDebtScore()));

            Map<Integer, BigDecimal> repIem = iemByRepByStage.getOrDefault(mcr.getRepresentativeId(), Map.of());
            for (Stage stage : stages) {
                row.append(DELIMITER);
                BigDecimal iem = repIem.get(stage.getId());
                row.append(iem != null ? IsepMath.toPercent(iem).toPlainString() : "");
            }

            csv.append(LINE_SEP).append(row);
        }

        return csv.toString();
    }

    private String buildHeader(List<String> stageNames) {
        StringBuilder header = new StringBuilder(
                "Projeto" + DELIMITER +
                "Questionário" + DELIMITER +
                "Iteração/Etapa" + DELIMITER +
                "Calculado em" + DELIMITER +
                "ID Membro" + DELIMITER +
                "Nome Membro" + DELIMITER +
                "ICP (%)" + DELIMITER +
                "Faixa ICP" + DELIMITER +
                "ISEP Iteração (%)" + DELIMITER +
                "Faixa ISEP" + DELIMITER +
                "Ética (%)" + DELIMITER +
                "Processo (%)" + DELIMITER +
                "Equidade (%)" + DELIMITER +
                "ESG (%)" + DELIMITER +
                "Dívida Ética (%)" + DELIMITER +
                "Dívida Técnica (%)"
        );
        for (String stage : stageNames) {
            header.append(DELIMITER).append("IEM ").append(escapeCsv(stage)).append(" (%)");
        }
        return header.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(DELIMITER) || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String percentOrEmpty(BigDecimal value) {
        return value != null ? IsepMath.toPercent(value).toPlainString() : "";
    }
}


package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.IsepDataExportDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportIsepDataUseCase {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final RepresentativeRepository representativeRepository;
    private final StageRepository stageRepository;
    private final IsepResultQueryPort isepResultQueryPort;

    @Transactional(readOnly = true)
    public IsepDataExportDTO execute(Long projectId, Integer questionnaireId, boolean anonymize) {
        log.info("[isep-export] Exportando dados questionário={} projeto={} anonymize={}", questionnaireId, projectId, anonymize);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + projectId));

        Questionnaire questionnaire = questionnaireRepository.findByIdAndProjectId(questionnaireId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionário não encontrado: " + questionnaireId));

        QuestionnaireResult result = isepResultQueryPort.findByQuestionnaireId(questionnaireId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resultado ISEP ainda não disponível para o questionário: " + questionnaireId));

        Map<Long, Representative> repMap = representativeRepository.findByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(Representative::getId, r -> r));

        Map<Integer, Stage> stageMap = stageRepository.findByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(Stage::getId, s -> s));

        Map<Long, Map<Integer, Double>> stageIemByRep = result.getStageResults().stream()
                .collect(Collectors.groupingBy(
                        MemberStageComplianceResult::getRepresentativeId,
                        Collectors.toMap(
                                MemberStageComplianceResult::getStageId,
                                mscr -> mscr.getIem() != null ? mscr.getIem().doubleValue() : 0.0
                        )
                ));

        List<IsepDataExportDTO.MemberExportRow> memberRows = result.getMemberResults().stream()
                .map(mcr -> {
                    Representative rep = repMap.get(mcr.getRepresentativeId());
                    String memberName = null;
                    if (!anonymize && rep != null && rep.getUser() != null) {
                        memberName = rep.getUser().getFirstName() + " " + rep.getUser().getLastName();
                    }

                    List<IsepDataExportDTO.StageIemExportRow> stageRows = stageIemByRep
                            .getOrDefault(mcr.getRepresentativeId(), Collections.emptyMap())
                            .entrySet().stream()
                            .map(e -> {
                                Stage stage = stageMap.get(e.getKey());
                                String stageName = stage != null ? stage.getName() : "Etapa " + e.getKey();
                                return new IsepDataExportDTO.StageIemExportRow(
                                        e.getKey(),
                                        stageName,
                                        IsepMath.toPercent(java.math.BigDecimal.valueOf(e.getValue()))
                                );
                            })
                            .sorted(Comparator.comparing(IsepDataExportDTO.StageIemExportRow::stageId))
                            .toList();

                    return new IsepDataExportDTO.MemberExportRow(
                            mcr.getRepresentativeId(),
                            memberName,
                            IsepMath.toPercent(mcr.getIcp()),
                            mcr.getBand(),
                            stageRows
                    );
                })
                .sorted(Comparator.comparing(IsepDataExportDTO.MemberExportRow::icpPercent).reversed())
                .toList();

        String iterationOrStageName = questionnaire.getIteration() != null
                ? questionnaire.getIteration()
                : (questionnaire.getStage() != null ? questionnaire.getStage().getName() : questionnaire.getName());

        return new IsepDataExportDTO(
                project.getId(),
                project.getName(),
                result.getQuestionnaireId(),
                questionnaire.getName(),
                iterationOrStageName,
                result.getCalculatedAt(),
                IsepMath.toPercent(result.getIsep()),
                result.getBand(),
                IsepMath.toPercent(result.getTeamSimpleAverage()),
                IsepMath.toPercent(result.getTeamStandardDeviation()),
                toPercent(result.getEthicsScore()),
                toPercent(result.getProcessScore()),
                toPercent(result.getFairnessScore()),
                toPercent(result.getEsgScore()),
                toPercent(result.getEthicsDebtScore()),
                toPercent(result.getTechDebtScore()),
                memberRows
        );
    }

    private BigDecimal toPercent(BigDecimal value) {
        return value != null ? IsepMath.toPercent(value) : null;
    }

    @Transactional(readOnly = true)
    public List<IsepDataExportDTO> executeAll(Long projectId, boolean anonymize) {
        log.info("[isep-export] Exportando todos os dados do projeto={} anonymize={}", projectId, anonymize);

        List<QuestionnaireResult> results = isepResultQueryPort.findByProjectId(projectId);
        return results.stream()
                .map(r -> {
                    try {
                        return execute(projectId, r.getQuestionnaireId(), anonymize);
                    } catch (ResourceNotFoundException ex) {
                        log.warn("[isep-export] Questionário={} não encontrado durante exportação do projeto={}",
                                r.getQuestionnaireId(), projectId);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}


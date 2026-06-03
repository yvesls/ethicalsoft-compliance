package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.IsepHistoryItemDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.ProjectIsepDashboardDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
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
public class GetProjectIsepDashboardUseCase {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final IsepResultQueryPort isepResultQueryPort;

    @Transactional(readOnly = true)
    public ProjectIsepDashboardDTO execute(Long projectId) {
        log.info("[dashboard-project] Montando dashboard consolidado projeto={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + projectId));

        List<Questionnaire> allQuestionnaires = questionnaireRepository.findByProjectId(projectId);
        List<QuestionnaireResult> completedResults = isepResultQueryPort.findByProjectId(projectId);

        Map<Integer, Questionnaire> questionnaireMap = allQuestionnaires.stream()
                .collect(Collectors.toMap(Questionnaire::getId, q -> q));

        List<IsepHistoryItemDTO> history = completedResults.stream()
                .sorted(Comparator.comparing(QuestionnaireResult::getCalculatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(r -> {
                    Questionnaire q = questionnaireMap.get(r.getQuestionnaireId());
                    return new IsepHistoryItemDTO(
                            r.getQuestionnaireId(),
                            q != null ? q.getName() : "Questionário " + r.getQuestionnaireId(),
                            q != null && q.getStage() != null ? q.getStage().getName() : null,
                            q != null ? q.getIteration() : null,
                            r.getIsep(),
                            IsepMath.toPercent(r.getIsep()),
                            r.getBand(),
                            r.getCalculatedAt(),
                            toPercent(r.getEthicsDebtScore()),
                            toPercent(r.getTechDebtScore())
                    );
                })
                .collect(Collectors.toList());

        BigDecimal projectIsep = null;
        String projectBand = null;
        BigDecimal projectIsepPercent = null;
        BigDecimal teamAvgPercent = null;
        BigDecimal teamStdDevPercent = null;

        BigDecimal ethicsPercent = null;
        BigDecimal processPercent = null;
        BigDecimal fairnessPercent = null;
        BigDecimal esgPercent = null;
        BigDecimal ethicsDebtPercent = null;
        BigDecimal techDebtPercent = null;

        if (!completedResults.isEmpty()) {
            List<IsepMath.WeightedValue> weightedValues = completedResults.stream()
                    .map(r -> {
                        Questionnaire q = questionnaireMap.get(r.getQuestionnaireId());
                        BigDecimal weight = (q != null && q.getWeight() != null)
                                ? BigDecimal.valueOf(q.getWeight())
                                : BigDecimal.ONE;
                        return new IsepMath.WeightedValue(r.getIsep(), weight);
                    })
                    .collect(Collectors.toList());

            projectIsep = IsepMath.weightedAverage(weightedValues);
            projectIsepPercent = IsepMath.toPercent(projectIsep);
            projectBand = EthicalComplianceBand.classify(projectIsepPercent).name();

            Collection<BigDecimal> isepValues = completedResults.stream()
                    .map(QuestionnaireResult::getIsep).toList();
            teamAvgPercent = IsepMath.toPercent(IsepMath.simpleAverage(isepValues));
            teamStdDevPercent = IsepMath.toPercent(IsepMath.standardDeviation(isepValues));

            ethicsPercent = averagePercentNonNull(completedResults.stream().map(QuestionnaireResult::getEthicsScore).toList());
            processPercent = averagePercentNonNull(completedResults.stream().map(QuestionnaireResult::getProcessScore).toList());
            fairnessPercent = averagePercentNonNull(completedResults.stream().map(QuestionnaireResult::getFairnessScore).toList());
            esgPercent = averagePercentNonNull(completedResults.stream().map(QuestionnaireResult::getEsgScore).toList());
            ethicsDebtPercent = averagePercentNonNull(completedResults.stream().map(QuestionnaireResult::getEthicsDebtScore).toList());
            techDebtPercent = averagePercentNonNull(completedResults.stream().map(QuestionnaireResult::getTechDebtScore).toList());

            log.info("[dashboard-project] ISEP Consolidado projeto={} isep={}% faixa={}",
                    projectId, projectIsepPercent, projectBand);
        }

        boolean projectClosed = project.getStatus() == ProjectStatusEnum.CONCLUIDO;
        boolean bulletinEligible = projectClosed && projectBand != null
                && !EthicalComplianceBand.meetsMinimum(projectBand);
        boolean certificateEligible = projectClosed && projectBand != null
                && EthicalComplianceBand.meetsMinimum(projectBand);

        return new ProjectIsepDashboardDTO(
                project.getId(),
                project.getName(),
                project.getType() != null ? project.getType().name() : null,
                projectIsep,
                projectIsepPercent,
                projectBand,
                teamAvgPercent,
                teamStdDevPercent,
                history,
                allQuestionnaires.size(),
                completedResults.size(),
                ethicsPercent,
                processPercent,
                fairnessPercent,
                esgPercent,
                ethicsDebtPercent,
                techDebtPercent,
                bulletinEligible,
                certificateEligible
        );
    }

    private BigDecimal toPercent(BigDecimal value) {
        return value != null ? IsepMath.toPercent(value) : null;
    }

    private BigDecimal averagePercentNonNull(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(Objects::nonNull).toList();
        return nonNull.isEmpty() ? null : IsepMath.toPercent(IsepMath.simpleAverage(nonNull));
    }
}


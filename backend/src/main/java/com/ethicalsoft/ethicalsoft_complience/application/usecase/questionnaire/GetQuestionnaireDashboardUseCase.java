package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.MemberComplianceDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.QuestionnaireIsepDashboardDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.StageComplianceDTO;
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
public class GetQuestionnaireDashboardUseCase {

    private final QuestionnaireRepository questionnaireRepository;
    private final RepresentativeRepository representativeRepository;
    private final StageRepository stageRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final IsepResultQueryPort isepResultQueryPort;

    @Transactional(readOnly = true)
    public QuestionnaireIsepDashboardDTO execute(Long projectId, Integer questionnaireId) {
        log.info("[dashboard] Montando visão de gestão questionário={} projeto={}", questionnaireId, projectId);

        Questionnaire questionnaire = questionnaireRepository
                .findByIdAndProjectId(questionnaireId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questionário não encontrado: " + questionnaireId));

        QuestionnaireResult result = isepResultQueryPort
                .findByQuestionnaireId(questionnaireId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resultado ISEP ainda não calculado para o questionário: " + questionnaireId));

        Map<Long, Representative> repMap = representativeRepository
                .findByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(Representative::getId, r -> r));

        Map<Integer, Stage> stageMap = stageRepository
                .findByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(Stage::getId, s -> s));

        List<MemberComplianceDTO> memberResults = result.getMemberResults().stream()
                .map(mcr -> {
                    Representative rep = repMap.get(mcr.getRepresentativeId());
                    String name = rep != null && rep.getUser() != null
                            ? rep.getUser().getFirstName() + " " + rep.getUser().getLastName()
                            : "Representante " + mcr.getRepresentativeId();
                    return new MemberComplianceDTO(
                            mcr.getRepresentativeId(),
                            name,
                            mcr.getIcp(),
                            IsepMath.toPercent(mcr.getIcp()),
                            mcr.getBand()
                    );
                })
                .toList();

        List<StageComplianceDTO> stageResults = result.getStageResults().stream()
                .map(mscr -> {
                    Representative rep = repMap.get(mscr.getRepresentativeId());
                    Stage stage = stageMap.get(mscr.getStageId());
                    String repName = rep != null && rep.getUser() != null
                            ? rep.getUser().getFirstName() + " " + rep.getUser().getLastName()
                            : "Representante " + mscr.getRepresentativeId();
                    String stageName = stage != null ? stage.getName() : "Etapa " + mscr.getStageId();
                    return new StageComplianceDTO(
                            mscr.getRepresentativeId(),
                            repName,
                            mscr.getStageId(),
                            stageName,
                            mscr.getIem(),
                            IsepMath.toPercent(mscr.getIem()),
                            EthicalComplianceBand.classify(IsepMath.toPercent(mscr.getIem())).name()
                    );
                })
                .toList();

        Map<String, Long> bandDistribution = Arrays.stream(EthicalComplianceBand.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        band -> memberResults.stream()
                                .filter(m -> band.name().equals(m.band()))
                                .count(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<String> justifications = Collections.emptyList();

        return new QuestionnaireIsepDashboardDTO(
                result.getQuestionnaireId(),
                questionnaire.getName(),
                questionnaire.getStage() != null ? questionnaire.getStage().getName() : null,
                questionnaire.getIteration(),
                result.getIsep(),
                IsepMath.toPercent(result.getIsep()),
                result.getBand(),
                result.getTeamSimpleAverage(),
                IsepMath.toPercent(result.getTeamSimpleAverage()),
                result.getTeamStandardDeviation(),
                IsepMath.toPercent(result.getTeamStandardDeviation()),
                result.getCalculatedAt(),
                bandDistribution,
                memberResults,
                stageResults,
                justifications,
                toPercent(result.getEthicsScore()),
                toPercent(result.getProcessScore()),
                toPercent(result.getFairnessScore()),
                toPercent(result.getEsgScore()),
                toPercent(result.getEthicsDebtScore()),
                toPercent(result.getTechDebtScore()),
                !EthicalComplianceBand.meetsMinimum(result.getBand())
        );
    }

    private BigDecimal toPercent(BigDecimal value) {
        return value != null ? IsepMath.toPercent(value) : null;
    }
}


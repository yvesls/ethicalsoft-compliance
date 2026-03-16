package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.MemberComplianceResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.IndividualDashboardDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.IsepHistoryItemDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetIndividualDashboardUseCase {

    private final QuestionnaireRepository questionnaireRepository;
    private final RepresentativeRepository representativeRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final IsepResultQueryPort isepResultQueryPort;

    @Transactional(readOnly = true)
    public IndividualDashboardDTO execute(Long projectId, Integer questionnaireId, Long representativeId) {
        log.info("[dashboard-individual] representante={} questionário={} projeto={}",
                representativeId, questionnaireId, projectId);

        Representative rep = representativeRepository.findById(representativeId)
                .filter(r -> r.getProject() != null && projectId.equals(r.getProject().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Representante não encontrado no projeto: " + representativeId));

        String repName = rep.getUser() != null
                ? rep.getUser().getFirstName() + " " + rep.getUser().getLastName()
                : "Representante";

        Optional<QuestionnaireResult> resultOpt = isepResultQueryPort.findByQuestionnaireId(questionnaireId);

        BigDecimal personalIcp;
        String personalBand;
        BigDecimal teamAnonymousAvg;
        BigDecimal teamAnonymousAvgPercent;

        if (resultOpt.isPresent()) {
            QuestionnaireResult result = resultOpt.get();
            MemberComplianceResult memberResult = result.getMemberResults().stream()
                    .filter(m -> representativeId.equals(m.getRepresentativeId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sem resultado ISEP para o representante " + representativeId
                                    + " no questionário " + questionnaireId));
            personalIcp = memberResult.getIcp();
            personalBand = memberResult.getBand();
            teamAnonymousAvg = result.getTeamSimpleAverage();
            teamAnonymousAvgPercent = IsepMath.toPercent(teamAnonymousAvg);
        } else {
            QuestionnaireResponse raw = responseRepository
                    .findByProjectIdAndQuestionnaireIdAndRepresentativeId(projectId, questionnaireId, representativeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Respostas não encontradas para o representante " + representativeId
                                    + " no questionário " + questionnaireId));

            if (!QuestionnaireResponseStatus.COMPLETED.equals(raw.getStatus())) {
                throw new ResourceNotFoundException(
                        "O representante " + representativeId + " ainda não concluiu o questionário " + questionnaireId);
            }

            personalIcp = computeIcpFromRaw(raw);
            personalBand = EthicalComplianceBand.classify(IsepMath.toPercent(personalIcp)).name();
            teamAnonymousAvg = null;
            teamAnonymousAvgPercent = null;

            log.info("[dashboard-individual] ICP calculado em tempo-real: rep={} icp={}", representativeId, personalIcp);
        }

        BigDecimal personalIcpPercent = IsepMath.toPercent(personalIcp);

        List<QuestionnaireResult> allProjectResults = isepResultQueryPort.findByProjectId(projectId);

        List<BigDecimal> historicalIcps = allProjectResults.stream()
                .flatMap(r -> r.getMemberResults().stream())
                .filter(m -> representativeId.equals(m.getRepresentativeId()))
                .map(MemberComplianceResult::getIcp)
                .collect(Collectors.toList());

        boolean currentAlreadyInHistory = resultOpt.isPresent()
                && allProjectResults.stream().anyMatch(r -> questionnaireId.equals(r.getQuestionnaireId()));
        if (!currentAlreadyInHistory) {
            historicalIcps = new ArrayList<>(historicalIcps);
            historicalIcps.add(personalIcp);
        }

        BigDecimal personalHistoricalAvg = IsepMath.simpleAverage(historicalIcps);

        List<IsepHistoryItemDTO> personalEvolution = allProjectResults.stream()
                .flatMap(r -> r.getMemberResults().stream()
                        .filter(m -> representativeId.equals(m.getRepresentativeId()))
                        .map(m -> {
                            Questionnaire q = questionnaireRepository
                                    .findById(r.getQuestionnaireId()).orElse(null);
                            return new IsepHistoryItemDTO(
                                    r.getQuestionnaireId(),
                                    q != null ? q.getName() : "Questionário " + r.getQuestionnaireId(),
                                    q != null && q.getStage() != null ? q.getStage().getName() : null,
                                    q != null ? q.getIteration() : null,
                                    m.getIcp(),
                                    IsepMath.toPercent(m.getIcp()),
                                    m.getBand(),
                                    r.getCalculatedAt()
                            );
                        }))
                .sorted(Comparator.comparing(IsepHistoryItemDTO::calculatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        return new IndividualDashboardDTO(
                representativeId,
                repName,
                personalIcp,
                personalIcpPercent,
                personalBand,
                personalHistoricalAvg,
                IsepMath.toPercent(personalHistoricalAvg),
                teamAnonymousAvg,
                teamAnonymousAvgPercent,
                personalEvolution
        );
    }

    private BigDecimal computeIcpFromRaw(QuestionnaireResponse response) {
        if (response.getAnswers() == null || response.getAnswers().isEmpty()) {
            return BigDecimal.ZERO;
        }
        long simCount = response.getAnswers().stream()
                .filter(a -> Boolean.TRUE.equals(a.getResponse()))
                .count();
        return IsepMath.ratio(simCount, response.getAnswers().size());
    }
}

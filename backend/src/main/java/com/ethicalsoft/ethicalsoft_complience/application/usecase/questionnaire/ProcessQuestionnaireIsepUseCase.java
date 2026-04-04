package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepCalculationResult;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepCalculationStrategy;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessQuestionnaireIsepUseCase {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final List<IsepCalculationStrategy> strategies;
    private final IsepResultCommandPort isepResultCommandPort;
    private final IsepResultQueryPort isepResultQueryPort;

    @Transactional
    public boolean processIfComplete(Long projectId, Integer questionnaireId) {
        log.info("[isep-orchestrator] Verificando conclusão do questionário={} projeto={}", questionnaireId, projectId);

        if (isepResultQueryPort.existsByQuestionnaireId(questionnaireId)) {
            log.info("[isep-orchestrator] ISEP já calculado para questionário={}", questionnaireId);
            return true;
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            log.warn("[isep-orchestrator] Projeto não encontrado: {}", projectId);
            return false;
        }

        Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId).orElse(null);
        if (questionnaire == null) {
            log.warn("[isep-orchestrator] Questionário não encontrado: {}", questionnaireId);
            return false;
        }

        List<QuestionnaireResponse> responses = responseRepository
                .findByProjectIdAndQuestionnaireIdExcludingTemplates(projectId, questionnaireId);

        if (!isFullyCompleted(project, questionnaire, responses)) {
            log.info("[isep-orchestrator] Questionário={} ainda não possui 100% das respostas. Aguardando.", questionnaireId);
            return false;
        }

        return calculate(project, questionnaire, responses);
    }

    @Transactional
    public boolean forceCalculate(Long projectId, Integer questionnaireId) {
        log.info("[isep-orchestrator] Cálculo forçado questionário={} projeto={}", questionnaireId, projectId);

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return false;

        Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId).orElse(null);
        if (questionnaire == null) return false;

        List<QuestionnaireResponse> responses = responseRepository
                .findByProjectIdAndQuestionnaireIdExcludingTemplates(projectId, questionnaireId);

        if (responses.isEmpty()) {
            log.warn("[isep-orchestrator] Nenhuma resposta encontrada para questionário={}", questionnaireId);
            return false;
        }

        return calculate(project, questionnaire, responses);
    }

    private boolean calculate(Project project, Questionnaire questionnaire, List<QuestionnaireResponse> responses) {
        IsepCalculationStrategy strategy = resolveStrategy(project);
        if (strategy == null) {
            log.error("[isep-orchestrator] Nenhuma estratégia encontrada para tipo={}", project.getType());
            return false;
        }

        List<Representative> representatives = new ArrayList<>(
                project.getRepresentatives() != null ? project.getRepresentatives() : Set.of());

        List<QuestionnaireResponse> completed = responses.stream()
                .filter(r -> QuestionnaireResponseStatus.COMPLETED.equals(r.getStatus()))
                .collect(Collectors.toList());

        if (completed.isEmpty()) {
            log.warn("[isep-orchestrator] Nenhuma resposta COMPLETED para questionário={}", questionnaire.getId());
            return false;
        }

        IsepCalculationResult result = strategy.calculate(project, questionnaire, completed, representatives);
        isepResultCommandPort.save(result);

        log.info("[isep-orchestrator] ISEP calculado: questionário={} ISEP={}% faixa={}",
                questionnaire.getId(),
                IsepMath.toPercent(result.questionnaireIsep()),
                result.questionnaireBand());

        return true;
    }

    private boolean isFullyCompleted(Project project, Questionnaire questionnaire,
                                     List<QuestionnaireResponse> responses) {
        Set<Long> representativeIds = project.getRepresentatives() == null
                ? Set.of()
                : project.getRepresentatives().stream()
                        .map(Representative::getId)
                        .collect(Collectors.toSet());

        if (representativeIds.isEmpty()) return false;

        Set<Long> completedRepresentatives = responses.stream()
                .filter(r -> QuestionnaireResponseStatus.COMPLETED.equals(r.getStatus()))
                .map(QuestionnaireResponse::getRepresentativeId)
                .collect(Collectors.toSet());

        boolean allCompleted = completedRepresentatives.containsAll(representativeIds);
        log.debug("[isep-orchestrator] Progresso questionário={}: {}/{} completed",
                questionnaire.getId(), completedRepresentatives.size(), representativeIds.size());
        return allCompleted;
    }

    private IsepCalculationStrategy resolveStrategy(Project project) {
        return strategies.stream()
                .filter(s -> s.supportedType().equals(project.getType()))
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<BigDecimal> calculateProjectIsep(Long projectId) {
        List<QuestionnaireResult> results = isepResultQueryPort.findByProjectId(projectId);
        if (results.isEmpty()) return java.util.Optional.empty();

        List<IsepMath.WeightedValue> weighted = results.stream()
                .map(r -> {
                    Questionnaire q = questionnaireRepository.findById(r.getQuestionnaireId()).orElse(null);
                    BigDecimal weight = (q != null && q.getWeight() != null)
                            ? BigDecimal.valueOf(q.getWeight())
                            : BigDecimal.ONE;
                    return new IsepMath.WeightedValue(r.getIsep(), weight);
                })
                .collect(Collectors.toList());

        BigDecimal projectIsep = IsepMath.weightedAverage(weighted);
        log.info("[isep-orchestrator] ISEP do Projeto id={} = {}% faixa={}",
                projectId, IsepMath.toPercent(projectIsep),
                EthicalComplianceBand.classify(IsepMath.toPercent(projectIsep)));
        return java.util.Optional.of(projectIsep);
    }
}


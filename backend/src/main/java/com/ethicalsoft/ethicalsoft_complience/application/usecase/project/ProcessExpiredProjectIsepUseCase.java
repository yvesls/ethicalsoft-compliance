package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.ProjectIsepResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.ProjectIsepResultCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.ProjectIsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessExpiredProjectIsepUseCase {

    private final ProjectRepository projectRepository;
    private final IsepResultQueryPort isepResultQueryPort;
    private final ProjectIsepResultCommandPort projectIsepResultCommandPort;
    private final ProjectIsepResultQueryPort projectIsepResultQueryPort;
    private final SendNotificationUseCase sendNotificationUseCase;

    @Transactional
    public void execute() {
        List<Project> expired = projectRepository.findExpiredWithoutIsepResult(
                LocalDate.now(), ProjectStatusEnum.ABERTO, TimelineStatusEnum.ATRASADO);

        if (expired.isEmpty()) {
            log.info("[project-isep-scheduler] Nenhum projeto em andamento com prazo expirado sem resultado ISEP consolidado.");
            return;
        }

        log.info("[project-isep-scheduler] Processando {} projeto(s) com prazo expirado...", expired.size());

        int concluded = 0;
        int markedDelayed = 0;
        int skipped = 0;

        for (Project project : expired) {
            try {
                boolean done = processIfAllQuestionnairesCompleted(project, "Sistema (Scheduler)");
                if (done) {
                    concluded++;
                } else {
                    markProjectAsDelayed(project);
                    markedDelayed++;
                    notifyProjectOverdue(project);
                }
            } catch (Exception ex) {
                skipped++;
                log.error("[project-isep-scheduler] Erro ao processar projeto id={}", project.getId(), ex);
            }
        }

        log.info("[project-isep-scheduler] Concluído: {} finalizados, {} atrasados, {} ignorados.",
                concluded, markedDelayed, skipped);
    }

    @Transactional
    public void tryFinalizeProjectAfterQuestionnaire(Long projectId) {
        Project project = projectRepository.findByIdWithRepresentativesAndQuestionnaires(projectId).orElse(null);
        if (project == null) {
            log.warn("[project-isep-scheduler] Projeto id={} não encontrado para verificação pós-questionário.", projectId);
            return;
        }

        if (projectIsepResultQueryPort.existsByProjectId(projectId)) {
            log.info("[project-isep-scheduler] Projeto id={} já possui ISEP consolidado.", projectId);
            return;
        }

        processIfAllQuestionnairesCompleted(project, "Sistema (auto-finalização)");
    }

    @Transactional
    public ProjectIsepResult forceCloseProject(Long projectId, String closedBy) {
        Project project = projectRepository.findByIdWithRepresentativesAndQuestionnaires(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado: " + projectId));

        List<com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult> results =
                isepResultQueryPort.findByProjectId(projectId);

        if (results.isEmpty()) {
            throw new IllegalStateException(
                    "Não é possível encerrar o projeto sem nenhum questionário calculado. " +
                    "Pelo menos um questionário deve ter respostas e ISEP calculado.");
        }

        log.info("[project-isep-scheduler] Encerramento manual do projeto id={} por '{}'", projectId, closedBy);
        return calculateAndPersistProjectIsep(project, results, closedBy);
    }

    private boolean processIfAllQuestionnairesCompleted(Project project, String closedBy) {
        Set<Integer> questionnaireIds = project.getQuestionnaires() == null
                ? Set.of()
                : project.getQuestionnaires().stream()
                        .map(Questionnaire::getId)
                        .collect(Collectors.toSet());

        if (questionnaireIds.isEmpty()) {
            log.warn("[project-isep-scheduler] Projeto id={} não possui questionários.", project.getId());
            return false;
        }

        List<QuestionnaireResult> completedResults =
                isepResultQueryPort.findByProjectId(project.getId());

        Set<Integer> completedQuestionnaireIds = completedResults.stream()
                .map(r -> r.getQuestionnaireId())
                .collect(Collectors.toSet());

        boolean allCompleted = completedQuestionnaireIds.containsAll(questionnaireIds);

        log.debug("[project-isep-scheduler] Projeto id={}: {}/{} questionários com ISEP calculado.",
                project.getId(), completedQuestionnaireIds.size(), questionnaireIds.size());

        if (!allCompleted) {
            return false;
        }

        calculateAndPersistProjectIsep(project, completedResults, closedBy);
        return true;
    }

    private ProjectIsepResult calculateAndPersistProjectIsep(
            Project project,
            List<QuestionnaireResult> questionnaireResults,
            String closedBy) {

        List<IsepMath.WeightedValue> weighted = new ArrayList<>();
        for (var qResult : questionnaireResults) {
            Questionnaire q = project.getQuestionnaires() == null ? null :
                    project.getQuestionnaires().stream()
                            .filter(qt -> qt.getId().equals(qResult.getQuestionnaireId()))
                            .findFirst().orElse(null);
            BigDecimal weight = (q != null && q.getWeight() != null)
                    ? q.getWeight()
                    : BigDecimal.ONE;
            weighted.add(new IsepMath.WeightedValue(qResult.getIseq(), weight));
        }

        BigDecimal consolidatedIsep = IsepMath.weightedAverage(weighted);
        BigDecimal isepPercent = IsepMath.toPercent(consolidatedIsep);
        EthicalComplianceBand band = EthicalComplianceBand.classify(isepPercent);

        Collection<BigDecimal> isepValues = questionnaireResults.stream()
                .map(QuestionnaireResult::getIseq)
                .toList();
        BigDecimal teamAvg = IsepMath.simpleAverage(isepValues);
        BigDecimal teamStdDev = IsepMath.standardDeviation(isepValues);

        BigDecimal avgEthics = averageNonNull(questionnaireResults.stream().map(QuestionnaireResult::getEthicsScore).toList());
        BigDecimal avgProcess = averageNonNull(questionnaireResults.stream().map(QuestionnaireResult::getProcessScore).toList());
        BigDecimal avgFairness = averageNonNull(questionnaireResults.stream().map(QuestionnaireResult::getFairnessScore).toList());
        BigDecimal avgEsg = averageNonNull(questionnaireResults.stream().map(QuestionnaireResult::getEsgScore).toList());
        BigDecimal avgEthicsDebt = averageNonNull(questionnaireResults.stream().map(QuestionnaireResult::getEthicsDebtScore).toList());
        BigDecimal avgTechDebt = averageNonNull(questionnaireResults.stream().map(QuestionnaireResult::getTechDebtScore).toList());

        log.info("[project-isep-scheduler] ISEP Consolidado do Projeto id={}: {}% (Faixa {})",
                project.getId(), isepPercent, band.name());

        ProjectIsepResult result = new ProjectIsepResult();
        result.setProjectId(project.getId());
        result.setIsep(consolidatedIsep);
        result.setBand(band.name());
        result.setQuestionnaireCount(questionnaireResults.size());
        result.setCalculatedAt(LocalDateTime.now());
        result.setClosedBy(closedBy);
        result.setTeamSimpleAverage(teamAvg);
        result.setTeamStandardDeviation(teamStdDev);
        result.setEthicsScore(avgEthics);
        result.setProcessScore(avgProcess);
        result.setFairnessScore(avgFairness);
        result.setEsgScore(avgEsg);
        result.setEthicsDebtScore(avgEthicsDebt);
        result.setTechDebtScore(avgTechDebt);

        ProjectIsepResult saved = projectIsepResultCommandPort.save(result);

        project.setStatus(ProjectStatusEnum.CONCLUIDO);
        project.setTimelineStatus(TimelineStatusEnum.CONCLUIDO);
        project.setClosingDate(LocalDate.now());
        projectRepository.save(project);

        notifyProjectIsepCalculated(project, saved);

        return saved;
    }

    private BigDecimal averageNonNull(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream()
                .filter(Objects::nonNull)
                .toList();
        return nonNull.isEmpty() ? null : IsepMath.simpleAverage(nonNull);
    }

    private void markProjectAsDelayed(Project project) {
        project.setTimelineStatus(TimelineStatusEnum.ATRASADO);
        projectRepository.save(project);
        log.warn("[project-isep-scheduler] Projeto id={} marcado como ATRASADO.", project.getId());
    }

    private void notifyProjectOverdue(Project project) {
        try {
            long totalCount = project.getQuestionnaires() == null ? 0 : project.getQuestionnaires().size();
            List<QuestionnaireResult> done =
                    isepResultQueryPort.findByProjectId(project.getId());
            long doneCount = done.size();
            long pendingCount = totalCount - doneCount;

            List<String> recipients = resolveAllRepresentantEmails(project);

            Map<String, Object> context = new HashMap<>();
            context.put("projectId", project.getId());
            context.put("projectName", project.getName());
            context.put("deadline", project.getDeadline());
            context.put("deadlineFormatted", project.getDeadline() != null ? project.getDeadline().toString() : "");
            context.put("pendingQuestionnaires", String.valueOf(pendingCount));
            context.put("totalQuestionnaires", String.valueOf(totalCount));
            context.put("projectLink", "/projects/" + project.getId());
            context.put("recipients", recipients);

            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.PROJECT_OVERDUE, context));

            log.info("[project-isep-scheduler] Notificação PROJETO ATRASADO enviada. projeto={}", project.getId());
        } catch (Exception ex) {
            log.error("[project-isep-scheduler] Falha ao notificar projeto atrasado id={}: {}",
                    project.getId(), ex.getMessage());
        }
    }

    void notifyProjectIsepCalculated(Project project, ProjectIsepResult result) {
        try {
            String isepPercent = IsepMath.toPercent(result.getIsep()).toPlainString();
            List<String> recipients = resolveAllRepresentantEmails(project);

            Map<String, Object> context = new HashMap<>();
            context.put("projectId", project.getId());
            context.put("projectName", project.getName());
            context.put("isepPercent", isepPercent);
            context.put("band", result.getBand());
            context.put("closedBy", result.getClosedBy());
            context.put("totalQuestionnaires", String.valueOf(result.getQuestionnaireCount()));
            context.put("calculatedAt", result.getCalculatedAt());
            context.put("calculatedAtFormatted", result.getCalculatedAt() != null ? result.getCalculatedAt().toString() : "");
            context.put("projectLink", "/projects/" + project.getId());

            if (result.getEthicsDebtScore() != null) {
                context.put("ethicsDebtPercent", IsepMath.toPercent(result.getEthicsDebtScore()).toPlainString());
            }
            if (result.getTechDebtScore() != null) {
                context.put("techDebtPercent", IsepMath.toPercent(result.getTechDebtScore()).toPlainString());
            }
            if (result.getEthicsScore() != null) {
                context.put("ethicsScorePercent", IsepMath.toPercent(result.getEthicsScore()).toPlainString());
            }
            if (result.getProcessScore() != null) {
                context.put("processScorePercent", IsepMath.toPercent(result.getProcessScore()).toPlainString());
            }
            context.put("recipients", recipients);

            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.PROJECT_ISEP_CALCULATED, context));

            log.info("[project-isep-scheduler] Notificação ISEP PROJETO CALCULADO enviada. ISEP={}% faixa={} projeto={}",
                    isepPercent, result.getBand(), project.getId());
        } catch (Exception ex) {
            log.error("[project-isep-scheduler] Falha ao notificar ISEP do projeto id={}: {}",
                    project.getId(), ex.getMessage());
        }
    }

    private List<String> resolveAllRepresentantEmails(Project project) {
        List<String> emails = new ArrayList<>();

        if (project.getOwner() != null && project.getOwner().getEmail() != null) {
            emails.add(project.getOwner().getEmail());
        }

        if (project.getRepresentatives() != null) {
            project.getRepresentatives().stream()
                    .filter(r -> r.getUser() != null && r.getUser().getEmail() != null)
                    .map(r -> r.getUser().getEmail())
                    .filter(e -> !emails.contains(e))
                    .forEach(emails::add);
        }

        return emails;
    }
}




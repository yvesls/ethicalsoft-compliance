package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.IterationRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.StageRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;

@Slf4j
public class ProjectTimelineStatusPolicy {

    private final Clock clock;
    private final StageRepository stageRepository;
    private final IterationRepository iterationRepository;
    private final QuestionnaireRepository questionnaireRepository;

    public ProjectTimelineStatusPolicy(Clock clock,
                                       StageRepository stageRepository,
                                       IterationRepository iterationRepository,
                                       QuestionnaireRepository questionnaireRepository) {
        this.clock = clock;
        this.stageRepository = stageRepository;
        this.iterationRepository = iterationRepository;
        this.questionnaireRepository = questionnaireRepository;
    }

    public void updateProjectTimeline(Project project) {
        if (project == null) {
            log.warn("[project-timeline-status-policy] Projeto nulo fornecido para atualização de timeline");
            return;
        }
        log.info("[project-timeline-status-policy] Atualizando status de timeline do projeto id={}", project.getId());

        LocalDate today = LocalDate.now(clock);

        String currentStage = null;
        Integer currentIterationIndex = null;

        Set<Iteration> iterations = loadIterations(project.getId(), project.getIterations());
        Set<Stage> stages = loadStages(project.getId(), project.getStages());
        var questionnaires = loadQuestionnaires(project.getId(), project.getQuestionnaires());

        if (project.getType() == ProjectTypeEnum.CASCATA) {
            currentStage = determineCurrentStage(stages, today);
        } else if (project.getType() == ProjectTypeEnum.ITERATIVO) {
            currentIterationIndex = determineCurrentIterationIndex(iterations, today);
        }

        project.setCurrentSituation(buildCurrentSituation(project, currentStage, currentIterationIndex));
        project.setTimelineStatus(resolveTimelineStatus(project.getStartDate(), project.getDeadline(), today, project.getTimelineStatus()));

        if (stages != null) {
            stages.forEach(stage -> stage.setStatus(resolveTimelineStatus(stage.getApplicationStartDate(), stage.getApplicationEndDate(), today, stage.getStatus())));
        }

        if (iterations != null) {
            iterations.forEach(iteration -> iteration.setStatus(resolveTimelineStatus(iteration.getApplicationStartDate(), iteration.getApplicationEndDate(), today, iteration.getStatus())));
        }

        if (questionnaires != null) {
            questionnaires.forEach(qn -> qn.setStatus(resolveTimelineStatus(qn.getApplicationStartDate(), qn.getApplicationEndDate(), today, qn.getStatus())));
        }
        log.info("[project-timeline-status-policy] Status de timeline atualizado para o projeto id={} status={}", project.getId(), project.getTimelineStatus());
    }

    private Set<Stage> loadStages(Long projectId, Set<Stage> currentStages) {
        if (currentStages != null && !currentStages.isEmpty()) {
            return currentStages;
        }
        if (projectId == null) {
            return currentStages;
        }
        return Set.copyOf(stageRepository.findByProjectId(projectId));
    }

    private Set<Iteration> loadIterations(Long projectId, Set<Iteration> currentIterations) {
        if (currentIterations != null && !currentIterations.isEmpty()) {
            return currentIterations;
        }
        if (projectId == null) {
            return currentIterations;
        }
        return Set.copyOf(iterationRepository.findByProjectId(projectId));
    }

    private Set<com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire> loadQuestionnaires(
            Long projectId,
            Set<com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire> currentQuestionnaires) {
        if (currentQuestionnaires != null && !currentQuestionnaires.isEmpty()) {
            return currentQuestionnaires;
        }
        if (projectId == null) {
            return currentQuestionnaires;
        }
        return Set.copyOf(questionnaireRepository.findByProjectId(projectId));
    }

    private TimelineStatusEnum resolveTimelineStatus(LocalDate start, LocalDate end, LocalDate today, TimelineStatusEnum currentStatus) {
        if (currentStatus == TimelineStatusEnum.CONCLUIDO) {
            return TimelineStatusEnum.CONCLUIDO;
        }
        if (start == null || end == null) {
            return TimelineStatusEnum.PENDENTE;
        }
        if (today.isBefore(start)) {
            return TimelineStatusEnum.PENDENTE;
        }
        if (!today.isAfter(end)) {
            return TimelineStatusEnum.EM_ANDAMENTO;
        }
        return TimelineStatusEnum.ATRASADO;
    }

    private String buildCurrentSituation(Project project, String currentStage, Integer currentIterationIndex) {
        if (project.getType() == ProjectTypeEnum.CASCATA) {
            return currentStage;
        }
        if (project.getType() == ProjectTypeEnum.ITERATIVO && currentIterationIndex != null && project.getIterationCount() != null) {
            return "Sprint " + currentIterationIndex + "/" + project.getIterationCount();
        }
        return null;
    }

    private String determineCurrentStage(Set<Stage> stages, LocalDate today) {
        if (stages == null || stages.isEmpty()) {
            return null;
        }
        return stages.stream()
                .filter(stage -> isWithinRange(stage.getApplicationStartDate(), stage.getApplicationEndDate(), today))
                .sorted(Comparator.comparing(Stage::getSequence))
                .map(Stage::getName)
                .findFirst()
                .orElse(null);
    }

    private Integer determineCurrentIterationIndex(Set<Iteration> iterations, LocalDate today) {
        if (iterations == null || iterations.isEmpty()) {
            return null;
        }
        var ordered = iterations.stream()
                .sorted(Comparator.comparing(Iteration::getApplicationStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (int i = 0; i < ordered.size(); i++) {
            Iteration iteration = ordered.get(i);
            if (isWithinRange(iteration.getApplicationStartDate(), iteration.getApplicationEndDate(), today)) {
                return i + 1;
            }
        }
        return null;
    }

    private boolean isWithinRange(LocalDate start, LocalDate end, LocalDate date) {
        if (start == null || end == null) {
            return false;
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }
}

package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;

@Service
@Slf4j
public class TimelineStatusService {

	public void updateProjectTimeline(Project project) {
		try {
			if (project == null) {
				log.warn("[timeline] Projeto nulo recebido na atualização de status");
				return;
			}
			log.info("[timeline] Atualizando status do projeto id={} tipo={} deadline={}",
					project.getId(), project.getType(), project.getDeadline());

			LocalDate today = LocalDate.now();

			String currentStage = null;
			Integer currentIterationIndex = null;

			if (project.getType() == ProjectTypeEnum.CASCATA) {
				currentStage = determineCurrentStage(project.getStages(), today);
				log.debug("[timeline] Projeto {} em etapa atual {}", project.getId(), currentStage);
			} else if (project.getType() == ProjectTypeEnum.ITERATIVO) {
				currentIterationIndex = determineCurrentIterationIndex(project.getIterations(), today);
				log.debug("[timeline] Projeto {} iteracao corrente {}", project.getId(), currentIterationIndex);
			}

			project.setCurrentSituation(buildCurrentSituation(project, currentStage, currentIterationIndex));
			project.setTimelineStatus(resolveTimelineStatus(project.getStartDate(), project.getDeadline(), today, project.getTimelineStatus()));

			if (project.getStages() != null) {
				project.getStages().forEach(stage -> stage.setStatus(resolveTimelineStatus(stage.getApplicationStartDate(), stage.getApplicationEndDate(), today, stage.getStatus())));
			}

			if (project.getIterations() != null) {
				project.getIterations().forEach(iteration -> iteration.setStatus(resolveTimelineStatus(iteration.getApplicationStartDate(), iteration.getApplicationEndDate(), today, iteration.getStatus())));
			}

			if (project.getQuestionnaires() != null) {
				project.getQuestionnaires().forEach(qn -> qn.setStatus(resolveTimelineStatus(qn.getApplicationStartDate(), qn.getApplicationEndDate(), today, qn.getStatus())));
			}

			log.info("[timeline] Projeto {} atualizado com status {} e situacao {}", project.getId(), project.getTimelineStatus(), project.getCurrentSituation());
		} catch (Exception ex) {
			log.error("[timeline] Falha ao atualizar status do projeto id={}",
					project != null ? project.getId() : null, ex);
			throw ex;
		}
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

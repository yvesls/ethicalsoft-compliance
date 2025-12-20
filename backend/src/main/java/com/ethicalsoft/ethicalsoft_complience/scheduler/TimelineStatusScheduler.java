package com.ethicalsoft.ethicalsoft_complience.scheduler;

import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.service.QuestionnaireReminderService;
import com.ethicalsoft.ethicalsoft_complience.service.TimelineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimelineStatusScheduler {

	private final ProjectRepository projectRepository;
	private final TimelineStatusService timelineStatusService;
	private final QuestionnaireRepository questionnaireRepository;
	private final QuestionnaireReminderService questionnaireService;

	@Scheduled(cron = "0 0 0 * * *")
	public void refreshTimelineStatuses() {
		var projects = projectRepository.findAllByOrderByIdAsc();
		projects.forEach(project -> {
			timelineStatusService.updateProjectTimeline(project);
			projectRepository.save(project);
		});
	}

	@Scheduled(cron = "0 0 6 * * *")
	public void notifyQuestionnaireStart() {
		var today = LocalDate.now();
		var starting = questionnaireRepository.findQuestionnairesStartingToday(today);
		starting.forEach(questionnaire -> {
			try {
				questionnaireService.sendAutomaticQuestionnaireReminder(questionnaire);
				log.info("[scheduler] Lembrete automático enviado para questionário {}", questionnaire.getId());
			} catch (Exception ex) {
				log.error("[scheduler] Falha ao enviar lembrete automático para questionário {}", questionnaire.getId(), ex);
			}
		});
	}
}

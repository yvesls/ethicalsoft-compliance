package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimelineStatusScheduler {

	private final ProjectRepository projectRepository;
	private final TimelineStatusService timelineStatusService;

	@Scheduled(cron = "0 0 0 * * *")
	public void refreshTimelineStatuses() {
		var projects = projectRepository.findAll();
		projects.forEach(timelineStatusService::updateProjectTimeline);
		projectRepository.saveAll(projects);
	}
}

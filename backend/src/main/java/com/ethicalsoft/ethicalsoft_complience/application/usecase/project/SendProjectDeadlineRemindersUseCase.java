package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendProjectDeadlineRemindersUseCase {

    private static final int DEFAULT_WINDOW_DAYS = 7;
    private final ProjectRepository projectRepository;
    private final SendNotificationUseCase sendNotificationUseCase;

    @Transactional(readOnly = true)
    public void execute() {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(DEFAULT_WINDOW_DAYS);
        List<Project> projects = projectRepository.findWithDeadlineBetween(today, until,
                com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum.CONCLUIDO);

        for (Project project : projects) {
            try {
                triggerReminder(project, today);
            } catch (Exception ex) {
                log.warn("[deadline-reminder] Falha ao notificar projeto id={} deadline={} ", project.getId(), project.getDeadline(), ex);
            }
        }
    }

    private void triggerReminder(Project project, LocalDate today) {
        if (project.getDeadline() == null) return;

        long daysRemaining = ChronoUnit.DAYS.between(today, project.getDeadline());
        Map<String, Object> context = new HashMap<>();
        context.put("projectId", project.getId());
        context.put("projectName", project.getName());
        context.put("deadline", project.getDeadline());
        context.put("daysRemaining", daysRemaining);

        List<String> recipients = new ArrayList<>();
        if (project.getOwner() != null && project.getOwner().getEmail() != null) {
            recipients.add(project.getOwner().getEmail());
        }
        for (Representative rep : project.getRepresentatives()) {
            if (rep.getUser() != null && rep.getUser().getEmail() != null) {
                recipients.add(rep.getUser().getEmail());
            }
        }
        context.put("recipients", recipients);

        sendNotificationUseCase.execute(new SendNotificationCommand(NotificationType.DEADLINE_REMINDER, context));
    }
}

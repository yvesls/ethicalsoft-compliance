package com.ethicalsoft.ethicalsoft_complience.infra.scheduler;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.project.ProcessExpiredProjectIsepUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.project.SendProjectDeadlineRemindersUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.ProcessExpiredQuestionnairesIsepUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.SendAutomaticQuestionnaireRemindersUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.timeline.RefreshAllProjectsTimelineStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimelineStatusScheduler {

    private final RefreshAllProjectsTimelineStatusUseCase refreshAllProjectsTimelineStatusUseCase;
    private final SendAutomaticQuestionnaireRemindersUseCase sendAutomaticQuestionnaireRemindersUseCase;
    private final SendProjectDeadlineRemindersUseCase sendProjectDeadlineRemindersUseCase;
    private final ProcessExpiredQuestionnairesIsepUseCase processExpiredQuestionnairesIsepUseCase;
    private final ProcessExpiredProjectIsepUseCase processExpiredProjectIsepUseCase;

    @Scheduled(cron = "0 0 0 * * *")
    public void refreshTimelineStatuses() {
        refreshAllProjectsTimelineStatusUseCase.execute();
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void notifyQuestionnaireStart() {
        try {
            sendAutomaticQuestionnaireRemindersUseCase.execute();
        } catch (Exception ex) {
            log.error("[scheduler] Falha ao executar lembretes automáticos", ex);
        }
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void notifyProjectDeadlines() {
        try {
            sendProjectDeadlineRemindersUseCase.execute();
        } catch (Exception ex) {
            log.error("[scheduler] Falha ao executar lembretes de deadline", ex);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void processExpiredQuestionnairesIsep() {
        try {
            processExpiredQuestionnairesIsepUseCase.execute();
        } catch (Exception ex) {
            log.error("[scheduler] Falha ao processar ISEP de questionários expirados", ex);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void processExpiredProjectsIsep() {
        try {
            processExpiredProjectIsepUseCase.execute();
        } catch (Exception ex) {
            log.error("[scheduler] Falha ao processar ISEP de projetos expirados", ex);
        }
    }
}


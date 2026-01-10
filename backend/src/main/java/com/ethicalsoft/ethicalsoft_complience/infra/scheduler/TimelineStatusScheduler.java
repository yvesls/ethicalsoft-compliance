package com.ethicalsoft.ethicalsoft_complience.infra.scheduler;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.timeline.RefreshAllProjectsTimelineStatusUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.SendAutomaticQuestionnaireRemindersUseCase;
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
}

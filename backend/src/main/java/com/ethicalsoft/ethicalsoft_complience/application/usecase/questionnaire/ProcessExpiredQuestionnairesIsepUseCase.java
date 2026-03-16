package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.project.ProcessExpiredProjectIsepUseCase;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessExpiredQuestionnairesIsepUseCase {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final ProcessQuestionnaireIsepUseCase processQuestionnaireIsepUseCase;
    private final IsepResultQueryPort isepResultQueryPort;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final ProcessExpiredProjectIsepUseCase processExpiredProjectIsepUseCase;

    @Transactional
    public void execute() {
        List<Questionnaire> expired = questionnaireRepository.findExpiredWithoutIsepResult(LocalDate.now());

        if (expired.isEmpty()) {
            log.info("[isep-scheduler] Nenhum questionário expirado sem resultado ISEP.");
            return;
        }

        log.info("[isep-scheduler] Processando {} questionários expirados...", expired.size());

        int processed = 0;
        int markedDelayed = 0;
        int skipped = 0;

        for (Questionnaire questionnaire : expired) {
            try {
                Long projectId = questionnaire.getProject() != null
                        ? questionnaire.getProject().getId()
                        : null;

                if (projectId == null) {
                    log.warn("[isep-scheduler] Questionário id={} sem projeto vinculado. Ignorado.", questionnaire.getId());
                    skipped++;
                    continue;
                }

                boolean done = processQuestionnaireIsepUseCase
                        .processIfComplete(projectId, questionnaire.getId());

                if (done) {
                    processed++;
                    log.info("[isep-scheduler] ISEP calculado para questionário id={} projeto id={}",
                            questionnaire.getId(), projectId);
                    notifyIsepCalculated(questionnaire, projectId, "Sistema (Scheduler)");
                    try {
                        processExpiredProjectIsepUseCase.tryFinalizeProjectAfterQuestionnaire(projectId);
                    } catch (Exception ex) {
                        log.error("[isep-scheduler] Erro ao verificar finalização do projeto id={} após questionário id={}",
                                projectId, questionnaire.getId(), ex);
                    }
                } else {
                    markAsDelayed(questionnaire);
                    markedDelayed++;
                    log.warn("[isep-scheduler] Questionário id={} expirado sem 100% de respostas. " +
                                    "Questionário e projeto id={} marcados como ATRASADO.",
                            questionnaire.getId(), projectId);
                    notifyOverdue(questionnaire, projectId);
                }
            } catch (Exception ex) {
                skipped++;
                log.error("[isep-scheduler] Erro ao processar questionário id={}",
                        questionnaire.getId(), ex);
            }
        }

        log.info("[isep-scheduler] Concluído: {} calculados, {} marcados como ATRASADO, {} ignorados.",
                processed, markedDelayed, skipped);
    }

    private void markAsDelayed(Questionnaire questionnaire) {
        questionnaire.setStatus(TimelineStatusEnum.ATRASADO);
        questionnaireRepository.save(questionnaire);
    }

    private void notifyOverdue(Questionnaire questionnaire, Long projectId) {
        try {
            var project = questionnaire.getProject();
            String projectName = project != null ? project.getName() : "";
            String adminEmail = (project != null && project.getOwner() != null)
                    ? project.getOwner().getEmail()
                    : null;

            int totalCount = project != null && project.getRepresentatives() != null
                    ? project.getRepresentatives().size()
                    : 0;

            List<QuestionnaireResponse> responses = questionnaireResponseRepository
                    .findByProjectIdAndQuestionnaireId(projectId, questionnaire.getId());
            long completedCount = responses.stream()
                    .filter(r -> QuestionnaireResponseStatus.COMPLETED.equals(r.getStatus()))
                    .map(QuestionnaireResponse::getRepresentativeId)
                    .distinct()
                    .count();
            long pendingCount = totalCount - completedCount;

            Map<String, Object> context = new HashMap<>();
            context.put("projectId", projectId);
            context.put("projectName", projectName);
            context.put("questionnaireName", questionnaire.getName());
            context.put("expiredAt", questionnaire.getApplicationEndDate());
            context.put("expiredAtFormatted", questionnaire.getApplicationEndDate() != null ? questionnaire.getApplicationEndDate().toString() : "");
            context.put("pendingCount", String.valueOf(pendingCount));
            context.put("totalCount", String.valueOf(totalCount));
            context.put("projectLink", "/projects/" + projectId);
            if (adminEmail != null) {
                context.put("recipients", List.of(adminEmail));
            }

            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.QUESTIONNAIRE_OVERDUE, context));

            log.info("[isep-scheduler] Notificação ATRASADO enviada. questionário={} projeto={}", questionnaire.getId(), projectId);
        } catch (Exception ex) {
            log.error("[isep-scheduler] Falha ao enviar notificação de ATRASADO para questionário={}: {}",
                    questionnaire.getId(), ex.getMessage());
        }
    }

    void notifyIsepCalculated(Questionnaire questionnaire, Long projectId, String closedBy) {
        try {
            var result = isepResultQueryPort.findByQuestionnaireId(questionnaire.getId()).orElse(null);
            if (result == null) {
                log.warn("[isep-scheduler] Resultado ISEP não encontrado para notificação. questionário={}", questionnaire.getId());
                return;
            }

            var project = questionnaire.getProject();
            String projectName = project != null ? project.getName() : "";
            String adminEmail = (project != null && project.getOwner() != null)
                    ? project.getOwner().getEmail()
                    : null;

            String isepPercent = IsepMath.toPercent(result.getIsep()).toPlainString();
            String band = result.getBand() != null ? result.getBand() : "";

            Map<String, Object> context = new HashMap<>();
            context.put("projectId", projectId);
            context.put("projectName", projectName);
            context.put("questionnaireName", questionnaire.getName());
            context.put("isepPercent", isepPercent);
            context.put("band", band);
            context.put("closedBy", closedBy);
            LocalDateTime calculatedAt = result.getCalculatedAt() != null ? result.getCalculatedAt() : LocalDateTime.now();
            context.put("calculatedAt", calculatedAt);
            context.put("calculatedAtFormatted", calculatedAt.toString());
            context.put("projectLink", "/projects/" + projectId);
            if (adminEmail != null) {
                context.put("recipients", List.of(adminEmail));
            }

            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.QUESTIONNAIRE_ISEP_CALCULATED, context));

            log.info("[isep-scheduler] Notificação ISEP calculado enviada. questionário={} ISEP={}% faixa={} projeto={}",
                    questionnaire.getId(), isepPercent, band, projectId);
        } catch (Exception ex) {
            log.error("[isep-scheduler] Falha ao enviar notificação de ISEP calculado para questionário={}: {}",
                    questionnaire.getId(), ex.getMessage());
        }
    }
}

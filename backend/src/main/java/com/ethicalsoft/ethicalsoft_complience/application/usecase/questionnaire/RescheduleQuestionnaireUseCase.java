package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.RescheduleQuestionnaireRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RescheduleQuestionnaireResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.*;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RescheduleQuestionnaireUseCase {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResultRepository questionnaireResultRepository;
    private final StageRepository stageRepository;
    private final IterationRepository iterationRepository;
    private final ProjectRepository projectRepository;
    private final SendNotificationUseCase sendNotificationUseCase;

    @Transactional
    public RescheduleQuestionnaireResponseDTO execute(Long projectId, Integer questionnaireId,
                                                      RescheduleQuestionnaireRequestDTO request) {
        log.info("[reschedule] Reagendando questionário id={} projeto={} novaInicio={} novoFim={}",
                questionnaireId, projectId, request.getNewApplicationStartDate(), request.getNewApplicationEndDate());

        Questionnaire questionnaire = questionnaireRepository.findByIdAndProjectId(questionnaireId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questionário id=" + questionnaireId + " não encontrado no projeto id=" + projectId));

        Project project = questionnaire.getProject();
        if (project == null) {
            project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Projeto id=" + projectId + " não encontrado."));
        }

        validateCanReschedule(questionnaire, project);
        validateDates(request, project);

        LocalDate oldStart = questionnaire.getApplicationStartDate();
        LocalDate oldEnd = questionnaire.getApplicationEndDate();

        questionnaire.setApplicationStartDate(request.getNewApplicationStartDate());
        questionnaire.setApplicationEndDate(request.getNewApplicationEndDate());

        TimelineStatusEnum newStatus = resolveNewStatus(
                request.getNewApplicationStartDate(), request.getNewApplicationEndDate(), LocalDate.now());
        questionnaire.setStatus(newStatus);
        questionnaireRepository.save(questionnaire);

        String stageOrIterationName = null;
        LocalDate stageOrIterationNewStart = null;
        LocalDate stageOrIterationNewEnd = null;

        if (project.getType() == ProjectTypeEnum.CASCATA && questionnaire.getStage() != null) {
            Stage stage = questionnaire.getStage();
            stageOrIterationName = stage.getName();
            updateStageFromQuestionnaires(stage);
            stageOrIterationNewStart = stage.getApplicationStartDate();
            stageOrIterationNewEnd = stage.getApplicationEndDate();
            stageRepository.save(stage);
        } else if (project.getType() == ProjectTypeEnum.ITERATIVO && questionnaire.getIterationRef() != null) {
            Iteration iteration = questionnaire.getIterationRef();
            stageOrIterationName = iteration.getName();
            updateIterationFromQuestionnaires(iteration);
            stageOrIterationNewStart = iteration.getApplicationStartDate();
            stageOrIterationNewEnd = iteration.getApplicationEndDate();
            iterationRepository.save(iteration);
        }

        boolean projectDeadlineExceeded = false;
        String projectDeadlineWarning = null;
        if (project.getDeadline() != null && request.getNewApplicationEndDate().isAfter(project.getDeadline())) {
            projectDeadlineExceeded = true;
            long daysOver = java.time.temporal.ChronoUnit.DAYS.between(project.getDeadline(), request.getNewApplicationEndDate());
            projectDeadlineWarning = String.format(
                    "A nova data de término do questionário (%s) ultrapassa o prazo do projeto (%s) em %d dia(s). " +
                    "Considere estender o prazo do projeto ou reduzir a duração das próximas etapas/iterações.",
                    request.getNewApplicationEndDate().format(BR_DATE),
                    project.getDeadline().format(BR_DATE),
                    daysOver);
        }

        List<String> notificationsSent = new ArrayList<>();
        try {
            sendRescheduleNotification(project, questionnaire, oldStart, oldEnd, request, projectDeadlineExceeded, projectDeadlineWarning);
            notificationsSent.add("QUESTIONNAIRE_RESCHEDULED (admin + representantes)");
        } catch (Exception ex) {
            log.error("[reschedule] Falha ao enviar notificação de reagendamento: {}", ex.getMessage());
        }

        if (projectDeadlineExceeded) {
            try {
                sendDeadlineExceededNotification(project, questionnaire, request, projectDeadlineWarning);
                notificationsSent.add("PROJECT_DEADLINE_EXCEEDED_WARNING (admin)");
            } catch (Exception ex) {
                log.error("[reschedule] Falha ao enviar notificação de prazo excedido: {}", ex.getMessage());
            }
        }

        log.info("[reschedule] Questionário id={} reagendado com sucesso. Novo status={}", questionnaireId, newStatus);

        return RescheduleQuestionnaireResponseDTO.builder()
                .questionnaireId(questionnaireId)
                .questionnaireName(questionnaire.getName())
                .oldStartDate(oldStart)
                .oldEndDate(oldEnd)
                .newStartDate(request.getNewApplicationStartDate())
                .newEndDate(request.getNewApplicationEndDate())
                .newStatus(newStatus.name())
                .stageOrIterationName(stageOrIterationName)
                .stageOrIterationNewStart(stageOrIterationNewStart)
                .stageOrIterationNewEnd(stageOrIterationNewEnd)
                .projectDeadlineExceeded(projectDeadlineExceeded)
                .projectDeadlineWarning(projectDeadlineWarning)
                .notificationsSent(notificationsSent)
                .build();
    }

    private void validateCanReschedule(Questionnaire questionnaire, Project project) {
        if (project.getStatus() == ProjectStatusEnum.CONCLUIDO) {
            throw new BusinessException("Não é possível reagendar questionários de um projeto concluído.");
        }
        if (project.getStatus() == ProjectStatusEnum.EXCLUIDO) {
            throw new BusinessException("Não é possível reagendar questionários de um projeto excluído.");
        }
        if (project.getStatus() == ProjectStatusEnum.RASCUNHO) {
            throw new BusinessException("Não é possível reagendar questionários de um projeto em rascunho. Edite o rascunho diretamente.");
        }

        boolean hasIsepResult = questionnaireResultRepository.findByQuestionnaireId(questionnaire.getId()).isPresent();
        if (hasIsepResult) {
            throw new BusinessException("Não é possível reagendar um questionário que já possui resultado ISEP calculado.");
        }

        if (questionnaire.getStatus() == TimelineStatusEnum.CONCLUIDO) {
            throw new BusinessException("Não é possível reagendar um questionário que já foi concluído.");
        }
    }

    private void validateDates(RescheduleQuestionnaireRequestDTO request, Project project) {
        if (request.getNewApplicationEndDate().isBefore(request.getNewApplicationStartDate())) {
            throw new BusinessException("A data de término não pode ser anterior à data de início.");
        }
        if (project.getStartDate() != null && request.getNewApplicationStartDate().isBefore(project.getStartDate())) {
            throw new BusinessException("A data de início do questionário não pode ser anterior à data de início do projeto (" +
                    project.getStartDate().format(BR_DATE) + ").");
        }
    }

    private TimelineStatusEnum resolveNewStatus(LocalDate start, LocalDate end, LocalDate today) {
        if (today.isBefore(start)) {
            return TimelineStatusEnum.PENDENTE;
        }
        if (!today.isAfter(end)) {
            return TimelineStatusEnum.EM_ANDAMENTO;
        }
        return TimelineStatusEnum.ATRASADO;
    }

    private void updateStageFromQuestionnaires(Stage stage) {
        if (stage.getQuestionnaires() == null || stage.getQuestionnaires().isEmpty()) return;

        LocalDate minStart = stage.getQuestionnaires().stream()
                .map(Questionnaire::getApplicationStartDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        LocalDate maxEnd = stage.getQuestionnaires().stream()
                .map(Questionnaire::getApplicationEndDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (minStart != null) stage.setApplicationStartDate(minStart);
        if (maxEnd != null) stage.setApplicationEndDate(maxEnd);

        stage.setStatus(resolveNewStatus(
                stage.getApplicationStartDate(), stage.getApplicationEndDate(), LocalDate.now()));
    }

    private void updateIterationFromQuestionnaires(Iteration iteration) {
        if (iteration.getQuestionnaires() == null || iteration.getQuestionnaires().isEmpty()) return;

        LocalDate minStart = iteration.getQuestionnaires().stream()
                .map(Questionnaire::getApplicationStartDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        LocalDate maxEnd = iteration.getQuestionnaires().stream()
                .map(Questionnaire::getApplicationEndDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (minStart != null) iteration.setApplicationStartDate(minStart);
        if (maxEnd != null) iteration.setApplicationEndDate(maxEnd);

        iteration.setStatus(resolveNewStatus(
                iteration.getApplicationStartDate(), iteration.getApplicationEndDate(), LocalDate.now()));
    }

    private void sendRescheduleNotification(Project project, Questionnaire questionnaire,
                                             LocalDate oldStart, LocalDate oldEnd,
                                             RescheduleQuestionnaireRequestDTO request,
                                             boolean deadlineExceeded, String deadlineWarning) {
        Map<String, Object> context = new HashMap<>();
        context.put("projectId", project.getId());
        context.put("projectName", project.getName());
        context.put("questionnaireName", questionnaire.getName());
        context.put("oldStartDate", oldStart != null ? oldStart.format(BR_DATE) : "N/A");
        context.put("oldEndDate", oldEnd != null ? oldEnd.format(BR_DATE) : "N/A");
        context.put("newStartDate", request.getNewApplicationStartDate().format(BR_DATE));
        context.put("newEndDate", request.getNewApplicationEndDate().format(BR_DATE));
        context.put("deadlineExceeded", deadlineExceeded);
        context.put("deadlineWarning", deadlineWarning != null ? deadlineWarning : "");
        context.put("projectLink", "/projects/" + project.getId());

        if (project.getOwner() != null) {
            context.put("recipients", List.of(project.getOwner().getEmail()));
        }

        sendNotificationUseCase.execute(new SendNotificationCommand(
                NotificationType.QUESTIONNAIRE_RESCHEDULED, context));
    }

    private void sendDeadlineExceededNotification(Project project, Questionnaire questionnaire,
                                                   RescheduleQuestionnaireRequestDTO request,
                                                   String warning) {
        Map<String, Object> context = new HashMap<>();
        context.put("projectId", project.getId());
        context.put("projectName", project.getName());
        context.put("questionnaireName", questionnaire.getName());
        context.put("newEndDate", request.getNewApplicationEndDate().format(BR_DATE));
        context.put("deadline", project.getDeadline() != null ? project.getDeadline().format(BR_DATE) : "N/A");
        context.put("warning", warning);
        context.put("projectLink", "/projects/" + project.getId());

        if (project.getOwner() != null) {
            context.put("recipients", List.of(project.getOwner().getEmail()));
        }

        sendNotificationUseCase.execute(new SendNotificationCommand(
                NotificationType.PROJECT_DEADLINE_EXCEEDED_WARNING, context));
    }
}


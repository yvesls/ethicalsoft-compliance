package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireReminderRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.service.criteria.QuestionnaireReminderContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireReminderService {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public void sendReminder(Long projectId, Integer questionnaireId, QuestionnaireReminderRequestDTO request) {
        try {
            log.info("[questionnaire] Enviando lembrete manual questionário={} projeto={} emails={}", questionnaireId, projectId, request != null ? request.emails() : null);


            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

            Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                    .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));

            if (!questionnaire.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Questionário não pertence ao projeto informado");
            }

            validateQuestionnaireInProgress(questionnaire);
            Set<String> targetEmails = resolveEmailsForReminder(questionnaire.getProject(), questionnaire.getId(), request);
            QuestionnaireReminderContext context = QuestionnaireReminderContext.from(questionnaire);

            sendReminderEmails(targetEmails, context);
        } catch ( Exception ex ) {
            log.error("[questionnaire] Falha ao enviar lembrete questionário={} projeto={}", questionnaireId, projectId, ex);
            throw ex;
        }
    }

    private void validateQuestionnaireInProgress(Questionnaire questionnaire) {
        if (isQuestionnaireInProgress(questionnaire)) {
            throw new BusinessException("Só é possível enviar lembretes para questionários em andamento.");
        }
    }

    @Transactional(readOnly = true)
    public void sendAutomaticQuestionnaireReminder(Questionnaire questionnaire) {
        if (isQuestionnaireInProgress(questionnaire)) {
            log.info("[questionnaire] Ignorando lembrete automático para questionário {} fora da vigência", questionnaire.getId());
            return;
        }
        Set<String> emails = resolveEmailsForReminder(questionnaire.getProject(), questionnaire.getId(), null);
        if (emails.isEmpty()) {
            log.info("[questionnaire] Nenhum email pendente para lembrete automático questionário {}", questionnaire.getId());
            return;
        }
        QuestionnaireReminderContext context = QuestionnaireReminderContext.from(questionnaire);
        sendReminderEmails(emails, context);
    }

    private boolean isQuestionnaireInProgress(Questionnaire questionnaire) {
        LocalDate today = LocalDate.now();
        boolean withinDates = questionnaire.getApplicationStartDate() != null
                && questionnaire.getApplicationEndDate() != null
                && !today.isBefore(questionnaire.getApplicationStartDate())
                && !today.isAfter(questionnaire.getApplicationEndDate());
        return !withinDates || questionnaire.getStatus() != TimelineStatusEnum.EM_ANDAMENTO;
    }


    private Set<String> resolveEmailsForReminder(Project project, Integer questionnaireId, QuestionnaireReminderRequestDTO request) {
        Set<String> manualEmails = request != null && request.emails() != null
                ? request.emails().stream().filter(Objects::nonNull).collect(Collectors.toSet())
                : Set.of();
        if (!manualEmails.isEmpty()) {
            return manualEmails;
        }
        List<QuestionnaireResponse> pendingResponses = questionnaireResponseRepository.findPendingResponses(project.getId(), questionnaireId);
        Set<Long> pendingRepresentativeIds = pendingResponses.stream()
                .map(QuestionnaireResponse::getRepresentativeId)
                .collect(Collectors.toSet());
        return Optional.ofNullable(project.getRepresentatives()).orElse(Set.of())
                .stream()
                .filter(rep -> pendingRepresentativeIds.contains(rep.getId()))
                .map(rep -> rep.getUser().getEmail())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void sendReminderEmails(Collection<String> emails, QuestionnaireReminderContext context) {
        emails.stream()
                .distinct()
                .forEach(email -> emailService.sendQuestionnaireReminderEmail(email, context));
    }
}

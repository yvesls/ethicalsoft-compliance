package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireReminderRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireReminderService {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireService questionnaireService;

    @Transactional(readOnly = true)
    public void sendReminder(Long projectId, Integer questionnaireId, QuestionnaireReminderRequestDTO request) {
        log.info("[questionnaire-reminder] Endpoint acionado para projeto {} questionário {}", projectId, questionnaireId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));
        Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));
        if (!questionnaire.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Questionário não pertence ao projeto informado");
        }
        questionnaireService.sendQuestionnaireReminder(projectId, questionnaireId, request);
    }
}

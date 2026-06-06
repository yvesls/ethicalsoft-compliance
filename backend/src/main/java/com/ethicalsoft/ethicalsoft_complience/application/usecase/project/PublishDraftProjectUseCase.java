package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.RepresentativeQuestionnaireResponseCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectTimelineStatusPolicy;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublishDraftProjectUseCase {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final RepresentativeRepository representativeRepository;
    private final ProjectTimelineStatusPolicy projectTimelineStatusPolicy;
    private final RepresentativeQuestionnaireResponseCommandPort representativeQuestionnaireResponseCommandPort;
    private final SendNotificationUseCase sendNotificationUseCase;

    @Transactional
    public ProjectResponseDTO execute(Long projectId) {
        log.info("[publish-draft] Publicando projeto rascunho id={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado id=" + projectId));

        if (project.getStatus() != ProjectStatusEnum.RASCUNHO) {
            throw new BusinessException("Somente projetos com status RASCUNHO podem ser publicados. Status atual: " + project.getStatus());
        }

        project.setStatus(ProjectStatusEnum.ABERTO);
        project.setTimelineStatus(TimelineStatusEnum.PENDENTE);

        var questionnaires = new HashSet<>(questionnaireRepository.findAllByProjectIdWithQuestions(project.getId()));
        project.setQuestionnaires(questionnaires);

        projectTimelineStatusPolicy.updateProjectTimeline(project);
        projectRepository.save(project);

        Set<Representative> representatives = new HashSet<>(representativeRepository.findByProjectId(projectId));
        project.setRepresentatives(representatives);

        validateRepresentativesRoles(project, representatives);
        createResponsesForRepresentatives(project, representatives);
        triggerInitialQuestionnaireReminders(project);

        log.info("[publish-draft] Projeto id={} publicado com sucesso. Status=ABERTO", projectId);

        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .type(project.getType().name())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .timelineStatus(project.getTimelineStatus())
                .currentSituation(project.getCurrentSituation())
                .representativeCount(representatives.size())
                .stageCount(project.getStages() != null ? project.getStages().size() : 0)
                .iterationCount(project.getIterations() != null ? project.getIterations().size() : 0)
                .build();
    }

    private void validateRepresentativesRoles(Project project, Set<Representative> representatives) {
        if (representatives == null || representatives.isEmpty()) {
            return;
        }
        if (project.getQuestionnaires() == null || project.getQuestionnaires().isEmpty()) {
            return;
        }
        for (Representative rep : representatives) {
            Set<Long> representativeRoleIds = Optional.ofNullable(rep.getRoles())
                    .orElseGet(Set::of)
                    .stream()
                    .map(Role::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (var questionnaire : project.getQuestionnaires()) {
                boolean hasMatchingRole = Optional.ofNullable(questionnaire.getQuestions())
                        .orElseGet(Set::of)
                        .stream()
                        .flatMap(question -> Optional.ofNullable(question.getRoles()).orElseGet(Set::of).stream())
                        .map(Role::getId)
                        .filter(Objects::nonNull)
                        .anyMatch(representativeRoleIds::contains);
                if (!hasMatchingRole) {
                    throw new BusinessException("Representante sem papéis vinculados às perguntas do questionário '" + questionnaire.getName() + "'.");
                }
            }
        }
    }

    private void createResponsesForRepresentatives(Project project, Set<Representative> representatives) {
        if (representatives == null || representatives.isEmpty()) {
            return;
        }
        representatives.forEach(rep ->
                representativeQuestionnaireResponseCommandPort.createResponsesForRepresentative(project, rep));
    }

    private void triggerInitialQuestionnaireReminders(Project project) {
        if (project.getQuestionnaires() == null) return;
        project.getQuestionnaires().forEach(q -> {
            try {
                sendNotificationUseCase.execute(new SendNotificationCommand(
                        NotificationType.QUESTIONNAIRE_REMINDER,
                        Map.ofEntries(
                                Map.entry("projectId", project.getId()),
                                Map.entry("questionnaireId", q.getId())
                        )
                ));
            } catch (Exception ex) {
                log.warn("[publish-draft] Falha ao disparar lembrete inicial projectId={} questionnaireId={}",
                        project.getId(), q.getId(), ex);
            }
        });
    }
}


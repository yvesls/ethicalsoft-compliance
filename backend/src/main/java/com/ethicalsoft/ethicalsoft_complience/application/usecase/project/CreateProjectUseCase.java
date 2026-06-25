package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.ProjectCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.RepresentativeQuestionnaireResponseCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.service.strategy.ProjectCreationStrategy;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.common.util.mapper.ModelMapperUtils;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectTimelineStatusPolicy;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreateProjectUseCase implements ProjectCommandPort {

    private final ProjectRepository projectRepository;
    private final CurrentUserPort currentUserPort;
    private final ProjectTimelineStatusPolicy projectTimelineStatusPolicy;
    private final AddRepresentativeUseCase addRepresentativeUseCase;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final QuestionnaireRepository questionnaireRepository;
    private final RepresentativeQuestionnaireResponseCommandPort representativeQuestionnaireResponseCommandPort;
    private final UpdateDraftProjectUseCase updateDraftProjectUseCase;
    private final PublishDraftProjectUseCase publishDraftProjectUseCase;

    private final Map<ProjectTypeEnum, ProjectCreationStrategy> strategyMap = new EnumMap<>(ProjectTypeEnum.class);

    public CreateProjectUseCase(ProjectRepository projectRepository,
                               CurrentUserPort currentUserPort,
                               ProjectTimelineStatusPolicy projectTimelineStatusPolicy,
                               AddRepresentativeUseCase addRepresentativeUseCase,
                               List<ProjectCreationStrategy> creationStrategies,
                               SendNotificationUseCase sendNotificationUseCase,
                               QuestionnaireRepository questionnaireRepository,
                               RepresentativeQuestionnaireResponseCommandPort representativeQuestionnaireResponseCommandPort,
                               UpdateDraftProjectUseCase updateDraftProjectUseCase,
                               PublishDraftProjectUseCase publishDraftProjectUseCase) {
        this.projectRepository = projectRepository;
        this.currentUserPort = currentUserPort;
        this.projectTimelineStatusPolicy = projectTimelineStatusPolicy;
        this.addRepresentativeUseCase = addRepresentativeUseCase;
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.questionnaireRepository = questionnaireRepository;
        this.representativeQuestionnaireResponseCommandPort = representativeQuestionnaireResponseCommandPort;
        this.updateDraftProjectUseCase = updateDraftProjectUseCase;
        this.publishDraftProjectUseCase = publishDraftProjectUseCase;

        if (creationStrategies != null) {
            creationStrategies.forEach(strategy -> this.strategyMap.put(strategy.getType(), strategy));
        }
    }

    @Override
    @Transactional
    public ProjectResponseDTO createProject(ProjectCreationRequestDTO request) {
        try {
            log.info("[usecase-create-project] Iniciando criação/atualização de projeto nome={} tipo={} status={} id={}",
                    request.getName(), request.getType(), request.getStatus(), request.getId());

            boolean requestedStatusIsOpen = request.getStatus() == ProjectStatusEnum.ABERTO;
            boolean requestedStatusIsDraft = request.getStatus() == null || request.getStatus() == ProjectStatusEnum.RASCUNHO;


            if (request.getId() != null) {
                return handleExistingDraft(request, requestedStatusIsOpen);
            }

            if (requestedStatusIsDraft) {
                return createNewDraft(request);
            }

            return createAndPublish(request);

        } catch (Exception ex) {
            log.error("[usecase-create-project] Falha ao criar/atualizar projeto nome={}", request != null ? request.getName() : null, ex);
            throw ex;
        }
    }

    private ProjectResponseDTO handleExistingDraft(ProjectCreationRequestDTO request, boolean publish) {
        Long projectId = request.getId();
        log.info("[usecase-create-project] Projeto id={} já existe. Atualizando rascunho. publish={}", projectId, publish);

        ProjectResponseDTO updated = updateDraftProjectUseCase.execute(projectId, request);

        if (publish) {
            log.info("[usecase-create-project] Publicando projeto rascunho id={}", projectId);
            return publishDraftProjectUseCase.execute(projectId);
        }

        return updated;
    }

    private ProjectResponseDTO createNewDraft(ProjectCreationRequestDTO request) {
        Project project = createProjectShell(request, ProjectStatusEnum.RASCUNHO);
        applyCreationStrategy(project, request);

        log.info("[usecase-create-project] Projeto salvo como RASCUNHO id={}", project.getId());

        Set<Representative> representatives = addRepresentativeUseCase.executeDraft(project, request.getRepresentatives());
        project.setRepresentatives(representatives);

        return buildResponse(project, representatives, request);
    }

    private ProjectResponseDTO createAndPublish(ProjectCreationRequestDTO request) {
        Project project = createProjectShell(request, ProjectStatusEnum.ABERTO);
        applyCreationStrategy(project, request);

        var questionnaires = new HashSet<>(questionnaireRepository.findAllByProjectIdWithQuestions(project.getId()));
        project.setQuestionnaires(questionnaires);
        project = refreshTimeline(project);

        Set<Representative> representatives = addRepresentativeUseCase.execute(project, request.getRepresentatives());
        project.setRepresentatives(representatives);

        validateRepresentativesRoles(project, representatives);
        createResponsesForRepresentatives(project, representatives);
        triggerInitialQuestionnaireRemindersAfterCommit(project);

        return buildResponse(project, representatives, request);
    }

    private Project createProjectShell(ProjectCreationRequestDTO request, ProjectStatusEnum status) {
        Project project = ModelMapperUtils.map(request, Project.class);
        project.setId(null);
        project.setOwner(currentUserPort.getCurrentUser());
        project.setType(ProjectTypeEnum.fromValue(request.getType()));
        project.setStages(new HashSet<>());
        project.setIterations(new HashSet<>());
        project.setRepresentatives(new HashSet<>());
        project.setQuestionnaires(new HashSet<>());
        project.setStatus(status);
        project.setTimelineStatus(TimelineStatusEnum.PENDENTE);
        project.setCurrentSituation(null);

        return projectRepository.save(java.util.Objects.requireNonNull(project));
    }

    private void applyCreationStrategy(Project project, ProjectCreationRequestDTO request) {
        ProjectCreationStrategy strategy = strategyMap.get(project.getType());
        if (strategy == null) {
            throw new IllegalArgumentException("Tipo de projeto não suportado: " + request.getType());
        }
        strategy.createStructure(project, request);
    }

    private Project refreshTimeline(Project project) {
        projectTimelineStatusPolicy.updateProjectTimeline(project);
        return projectRepository.save(project);
    }

    private ProjectResponseDTO buildResponse(Project project, Set<Representative> representatives, ProjectCreationRequestDTO request) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .type(project.getType().name())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .timelineStatus(project.getTimelineStatus())
                .currentSituation(project.getCurrentSituation())
                .representativeCount(representatives.size())
                .stageCount(request.getStages() != null ? request.getStages().size() : 0)
                .iterationCount(request.getIterations() != null ? request.getIterations().size() : 0)
                .build();
    }

    private void triggerInitialQuestionnaireRemindersAfterCommit(Project project) {
        if (project.getQuestionnaires() == null || project.getQuestionnaires().isEmpty()) {
            return;
        }

        Runnable action = () -> project.getQuestionnaires().forEach(q -> {
            try {
                sendNotificationUseCase.execute(new SendNotificationCommand(
                        NotificationType.QUESTIONNAIRE_REMINDER,
                        java.util.Map.ofEntries(
                                java.util.Map.entry("projectId", project.getId()),
                                java.util.Map.entry("questionnaireId", q.getId()),
                                java.util.Map.entry("systemTriggered", true)
                        )
                ));
            } catch (Exception ex) {
                log.warn("[usecase-create-project] Falha ao disparar lembrete inicial projectId={} questionnaireId={}", project.getId(), q.getId(), ex);
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void createResponsesForRepresentatives(Project project, Set<Representative> representatives) {
        if (representatives == null || representatives.isEmpty()) {
            return;
        }
        representatives.forEach(rep -> representativeQuestionnaireResponseCommandPort.createResponsesForRepresentative(project, rep));
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
                    .map(role -> role.getId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (var questionnaire : project.getQuestionnaires()) {
                boolean hasMatchingRole = Optional.ofNullable(questionnaire.getQuestions())
                        .orElseGet(Set::of)
                        .stream()
                        .flatMap(question -> Optional.ofNullable(question.getRoles()).orElseGet(Set::of).stream())
                        .map(role -> role.getId())
                        .filter(Objects::nonNull)
                        .anyMatch(representativeRoleIds::contains);
                if (!hasMatchingRole) {
                    throw new BusinessException("Representante sem papéis vinculados às perguntas do questionário '" + questionnaire.getName() + "'.");
                }
            }
        }
    }
}

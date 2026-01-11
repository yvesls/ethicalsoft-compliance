package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.QuestionnaireReminderPort;
import com.ethicalsoft.ethicalsoft_complience.application.service.strategy.ProjectCreationStrategy;
import com.ethicalsoft.ethicalsoft_complience.common.util.mapper.ModelMapperUtils;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectTimelineStatusPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreateProjectUseCase implements ProjectCommandPort {

    private final ProjectRepository projectRepository;
    private final CurrentUserPort currentUserPort;
    private final ProjectTimelineStatusPolicy projectTimelineStatusPolicy;
    private final AddRepresentativeUseCase addRepresentativeUseCase;
    private final QuestionnaireReminderPort questionnaireReminderPort;
    private final QuestionnaireRepository questionnaireRepository;

    private final Map<ProjectTypeEnum, ProjectCreationStrategy> strategyMap = new EnumMap<>(ProjectTypeEnum.class);

    public CreateProjectUseCase(ProjectRepository projectRepository,
                               CurrentUserPort currentUserPort,
                               ProjectTimelineStatusPolicy projectTimelineStatusPolicy,
                               AddRepresentativeUseCase addRepresentativeUseCase,
                               List<ProjectCreationStrategy> creationStrategies,
                               QuestionnaireReminderPort questionnaireReminderPort,
                               QuestionnaireRepository questionnaireRepository) {
        this.projectRepository = projectRepository;
        this.currentUserPort = currentUserPort;
        this.projectTimelineStatusPolicy = projectTimelineStatusPolicy;
        this.addRepresentativeUseCase = addRepresentativeUseCase;
        this.questionnaireReminderPort = questionnaireReminderPort;
        this.questionnaireRepository = questionnaireRepository;

        if (creationStrategies != null) {
            creationStrategies.forEach(strategy -> this.strategyMap.put(strategy.getType(), strategy));
        }
    }

    @Override
    @Transactional
    public ProjectResponseDTO createProject(ProjectCreationRequestDTO request) {
        try {
            log.info("[usecase-create-project] Iniciando criação de projeto nome={} tipo={}", request.getName(), request.getType());

            Project project = createProjectShell(request);
            applyCreationStrategy(project, request);

            var questionnaires = questionnaireRepository.findByProjectId(project.getId()).stream().collect(Collectors.toSet());
            project.setQuestionnaires(questionnaires);
            project = refreshTimeline(project);

            Set<Representative> representatives = addRepresentativeUseCase.execute(project, request.getRepresentatives());

            project.setRepresentatives(representatives);
            triggerInitialQuestionnaireReminders(project);

            return buildResponse(project, representatives, request);
        } catch (Exception ex) {
            log.error("[usecase-create-project] Falha ao criar projeto nome={}", request != null ? request.getName() : null, ex);
            throw ex;
        }
    }

    private Project createProjectShell(ProjectCreationRequestDTO request) {
        Project project = ModelMapperUtils.map(request, Project.class);
        project.setOwner(currentUserPort.getCurrentUser());
        project.setType(ProjectTypeEnum.fromValue(request.getType()));
        project.setStages(new HashSet<>());
        project.setIterations(new HashSet<>());
        project.setRepresentatives(new HashSet<>());
        project.setQuestionnaires(new HashSet<>());

        if (project.getStatus() == null) {
            project.setStatus(ProjectStatusEnum.RASCUNHO);
        }
        project.setTimelineStatus(TimelineStatusEnum.PENDENTE);
        project.setCurrentSituation(null);

        return projectRepository.save(project);
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

    private void triggerInitialQuestionnaireReminders(Project project) {
        project.getQuestionnaires().forEach(q -> {
            try {
                questionnaireReminderPort.sendAutomaticReminder(project.getId(), q.getId());
            } catch (Exception ex) {
                log.warn("[usecase-create-project] Falha ao disparar lembrete inicial projectId={} questionnaireId={}", project.getId(), q.getId(), ex);
            }
        });
    }

    private ProjectResponseDTO buildResponse(Project project, Set<Representative> representatives, ProjectCreationRequestDTO request) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .type(project.getType().name())
                .startDate(project.getStartDate())
                .timelineStatus(project.getTimelineStatus())
                .currentSituation(project.getCurrentSituation())
                .representativeCount(representatives.size())
                .stageCount(request.getStages() != null ? request.getStages().size() : 0)
                .iterationCount(request.getIterations() != null ? request.getIterations().size() : 0)
                .build();
    }
}
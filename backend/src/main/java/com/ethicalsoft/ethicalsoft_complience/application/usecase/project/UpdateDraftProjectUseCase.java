package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.application.service.strategy.ProjectCreationStrategy;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UpdateDraftProjectUseCase {

    private final ProjectRepository projectRepository;
    private final AddRepresentativeUseCase addRepresentativeUseCase;
    private final EntityManager entityManager;
    private final Map<ProjectTypeEnum, ProjectCreationStrategy> strategyMap = new EnumMap<>(ProjectTypeEnum.class);

    public UpdateDraftProjectUseCase(ProjectRepository projectRepository,
                                     AddRepresentativeUseCase addRepresentativeUseCase,
                                     EntityManager entityManager,
                                     List<ProjectCreationStrategy> creationStrategies) {
        this.projectRepository = projectRepository;
        this.addRepresentativeUseCase = addRepresentativeUseCase;
        this.entityManager = entityManager;

        if (creationStrategies != null) {
            creationStrategies.forEach(strategy -> this.strategyMap.put(strategy.getType(), strategy));
        }
    }

    @Transactional
    public ProjectResponseDTO execute(Long projectId, ProjectCreationRequestDTO request) {
        log.info("[update-draft] Atualizando projeto rascunho id={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado id=" + projectId));

        if (project.getStatus() != ProjectStatusEnum.RASCUNHO) {
            throw new BusinessException("Somente projetos com status RASCUNHO podem ser editados por esta operação. Status atual: " + project.getStatus());
        }

        project.setName(request.getName());
        project.setType(ProjectTypeEnum.fromValue(request.getType()));
        project.setStartDate(request.getStartDate());
        project.setDeadline(request.getDeadline());
        project.setIterationDuration(request.getIterationDuration());
        project.setIterationCount(request.getIterationCount());

        clearExistingStructure(projectId);

        project.setStages(new HashSet<>());
        project.setIterations(new HashSet<>());
        project.setQuestionnaires(new HashSet<>());
        projectRepository.save(project);

        ProjectCreationStrategy strategy = strategyMap.get(project.getType());
        if (strategy == null) {
            throw new BusinessException("Tipo de projeto não suportado: " + request.getType());
        }
        strategy.createStructure(project, request);

        var representatives = addRepresentativeUseCase.executeDraft(project, request.getRepresentatives());
        project.setRepresentatives(representatives);

        log.info("[update-draft] Projeto rascunho id={} atualizado com sucesso", projectId);

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

    private void clearExistingStructure(Long projectId) {
        entityManager.createQuery("DELETE FROM Question q WHERE q.questionnaire.project.id = :projectId")
                .setParameter("projectId", projectId).executeUpdate();
        entityManager.createQuery("DELETE FROM Questionnaire q WHERE q.project.id = :projectId")
                .setParameter("projectId", projectId).executeUpdate();
        entityManager.createQuery("DELETE FROM Iteration i WHERE i.project.id = :projectId")
                .setParameter("projectId", projectId).executeUpdate();
        entityManager.createQuery("DELETE FROM Stage s WHERE s.project.id = :projectId")
                .setParameter("projectId", projectId).executeUpdate();
        entityManager.createQuery("DELETE FROM Representative r WHERE r.project.id = :projectId")
                .setParameter("projectId", projectId).executeUpdate();
        entityManager.flush();
    }
}


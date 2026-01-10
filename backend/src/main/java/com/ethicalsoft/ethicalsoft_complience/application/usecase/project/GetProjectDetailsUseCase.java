package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectDetailResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentIterationPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentStagePolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectSituationPolicy;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetProjectDetailsUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectCurrentStagePolicy projectCurrentStagePolicy;
    private final ProjectCurrentIterationPolicy projectCurrentIterationPolicy;
    private final ProjectSituationPolicy projectSituationPolicy;

    @Transactional(readOnly = true)
    public ProjectDetailResponseDTO execute(Long projectId) {
        try {
            log.info("[usecase-get-project] Buscando detalhes do projeto id={}", projectId);
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

            return buildDetailResponse(project);
        } catch (Exception ex) {
            log.error("[usecase-get-project] Falha ao buscar projeto id={}", projectId, ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Project reloadAggregate(Long projectId) {
        return projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));
    }

    private ProjectDetailResponseDTO buildDetailResponse(Project project) {
        LocalDate now = LocalDate.now();
        String currentStage = null;
        Integer currentIteration = null;

        if (project.getType() == ProjectTypeEnum.CASCATA) {
            currentStage = projectCurrentStagePolicy.findCurrentStageName(project.getQuestionnaires(), now);
        } else if (project.getType() == ProjectTypeEnum.ITERATIVO) {
            currentIteration = projectCurrentIterationPolicy.findCurrentIterationNumber(project.getIterations(), now);
        }

        project.setCurrentSituation(projectSituationPolicy.buildCurrentSituation(project, currentStage, currentIteration));

        return ProjectDetailResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .type(project.getType() != null ? project.getType().name() : null)
                .startDate(project.getStartDate())
                .deadline(project.getDeadline())
                .closingDate(project.getClosingDate())
                .status(project.getStatus())
                .timelineStatus(project.getTimelineStatus())
                .iterationDuration(project.getIterationDuration())
                .configuredIterationCount(project.getIterationCount())
                .representativeCount(project.getRepresentatives() != null ? project.getRepresentatives().size() : 0)
                .stageCount(project.getStages() != null ? project.getStages().size() : 0)
                .iterationCount(project.getIterations() != null ? project.getIterations().size() : 0)
                .currentStage(currentStage)
                .currentIteration(currentIteration)
                .currentSituation(project.getCurrentSituation())
                .build();
    }
}
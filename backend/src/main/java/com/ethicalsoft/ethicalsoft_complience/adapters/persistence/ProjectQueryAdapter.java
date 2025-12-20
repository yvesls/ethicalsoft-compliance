package com.ethicalsoft.ethicalsoft_complience.adapters.persistence;

import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentIterationPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentStagePolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectSituationPolicy;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectDetailResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import com.ethicalsoft.ethicalsoft_complience.service.ProjectSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ProjectQueryAdapter implements ProjectQueryPort {

    private final ProjectRepository projectRepository;
    private final AuthService authService;
    private final ProjectCurrentStagePolicy projectCurrentStagePolicy;
    private final ProjectCurrentIterationPolicy projectCurrentIterationPolicy;
    private final ProjectSituationPolicy projectSituationPolicy;
    private final QuestionnaireRepository questionnaireRepository;

    @Override
    public Page<ProjectSummaryResponseDTO> search(ProjectSearchRequestDTO filters, Pageable pageable) {
        Specification<Project> spec = ProjectSpecification.findByCriteria(filters, authService.getAuthenticatedUser());
        Page<Project> projectPage = projectRepository.findAll(spec, pageable);
        LocalDate now = LocalDate.now();

        return projectPage.map(project -> {
            String currentStage = null;
            Integer currentIteration = null;

            if (project.getType() == ProjectTypeEnum.CASCATA) {
                currentStage = projectCurrentStagePolicy.findCurrentStageName(project.getQuestionnaires(), now);
            } else if (project.getType() == ProjectTypeEnum.ITERATIVO) {
                currentIteration = projectCurrentIterationPolicy.findCurrentIterationNumber(project.getIterations(), now);
            }

            return ProjectSummaryResponseDTO.builder()
                    .id(project.getId())
                    .name(project.getName())
                    .type(project.getType().name())
                    .status(project.getStatus())
                    .timelineStatus(project.getTimelineStatus())
                    .deadline(project.getDeadline())
                    .startDate(project.getStartDate())
                    .representativeCount(project.getRepresentatives() != null ? project.getRepresentatives().size() : 0)
                    .stageCount(project.getStages() != null ? project.getStages().size() : 0)
                    .iterationCount(project.getIterations() != null ? project.getIterations().size() : 0)
                    .currentStage(currentStage)
                    .currentIteration(currentIteration)
                    .currentSituation(projectSituationPolicy.buildCurrentSituation(project, currentStage, currentIteration))
                    .build();
        });
    }

    @Override
    public ProjectDetailResponseDTO getProjectDetail(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

        int representativeCount = project.getRepresentatives() != null ? project.getRepresentatives().size() : 0;
        int stageCount = project.getStages() != null ? project.getStages().size() : 0;
        int iterationCount = project.getIterations() != null ? project.getIterations().size() : 0;
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
                .representativeCount(representativeCount)
                .stageCount(stageCount)
                .iterationCount(iterationCount)
                .currentStage(currentStage)
                .currentIteration(currentIteration)
                .currentSituation(project.getCurrentSituation())
                .build();
    }

    @Override
    public Page<QuestionnaireSummaryResponseDTO> listQuestionnaires(Long projectId, Pageable pageable, QuestionnaireSearchFilter filter) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

        Specification<Questionnaire> spec = Specification.where((root, query, cb) -> cb.equal(root.get("project").get("id"), projectId));
        if (filter != null) {
            if (StringUtils.hasText(filter.name())) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(filter.stageName())) {
                spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("stage").get("name")), filter.stageName().toLowerCase()));
            }
            if (StringUtils.hasText(filter.iterationName())) {
                spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("iterationRef").get("name")), filter.iterationName().toLowerCase()));
            }
        }

        Page<Questionnaire> page = questionnaireRepository.findAll(spec, pageable);
        return page.map(questionnaire -> QuestionnaireSummaryResponseDTO.builder()
                .id(questionnaire.getId())
                .name(questionnaire.getName())
                .status(questionnaire.getStatus())
                .applicationStartDate(questionnaire.getApplicationStartDate())
                .applicationEndDate(questionnaire.getApplicationEndDate())
                .stageName(questionnaire.getStage() != null ? questionnaire.getStage().getName() : null)
                .iterationName(questionnaire.getIterationRef() != null ? questionnaire.getIterationRef().getName() : null)
                .respondents(null)
                .totalRespondents(null)
                .respondedRespondents(null)
                .pendingRespondents(null)
                .lastResponseAt(null)
                .progressStatus(null)
                .build());
    }
}

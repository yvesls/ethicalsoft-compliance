package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.query;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectDetailResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RespondentStatusDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.specification.ProjectSpecification;
import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.ProjectQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentIterationPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentStagePolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectSituationPolicy;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectQueryAdapter implements ProjectQueryPort {

    private final ProjectRepository projectRepository;
    private final ProjectCurrentStagePolicy projectCurrentStagePolicy;
    private final ProjectCurrentIterationPolicy projectCurrentIterationPolicy;
    private final ProjectSituationPolicy projectSituationPolicy;
    private final QuestionnaireRepository questionnaireRepository;
    private final CurrentUserPort currentUserPort;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;

    @Override
    public Page<ProjectSummaryResponseDTO> search(ProjectSearchRequestDTO filters, Pageable pageable) {
        User current = getCurrentUser();
        Specification<Project> spec = ProjectSpecification.findByCriteria(filters, current);
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

        Set<Representative> reps = Optional.ofNullable(project.getRepresentatives()).orElse(Set.of());
        Map<Long, Representative> representativesById = reps.stream()
                .collect(Collectors.toMap(Representative::getId, rep -> rep));

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
        return page.map(questionnaire -> buildQuestionnaireSummary(questionnaire, representativesById));
    }

    private QuestionnaireSummaryResponseDTO buildQuestionnaireSummary(Questionnaire questionnaire,
                                                                      Map<Long, Representative> representativesById) {
        List<QuestionnaireResponse> responses = questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireId(questionnaire.getProject().getId(), questionnaire.getId());

        Map<Long, QuestionnaireResponse> responseByRep = responses.stream()
                .filter(resp -> resp.getRepresentativeId() != null)
                .collect(Collectors.toMap(QuestionnaireResponse::getRepresentativeId, r -> r, (a, b) -> a));

        int totalRespondents = representativesById.size();
        AtomicInteger responded = new AtomicInteger();
        AtomicReference<LocalDateTime> lastResponseAt = new AtomicReference<>();
        List<RespondentStatusDTO> respondentStatus = representativesById.values().stream()
                .map(rep -> {
                    QuestionnaireResponse response = responseByRep.get(rep.getId());
                    QuestionnaireResponseStatus status = response != null ? response.getStatus() : QuestionnaireResponseStatus.PENDING;
                    LocalDateTime completedAt = response != null ? response.getSubmissionDate() : null;
                    if (status == QuestionnaireResponseStatus.COMPLETED) {
                        responded.getAndIncrement();
                        if (completedAt != null && (lastResponseAt.get() == null || completedAt.isAfter(lastResponseAt.get()))) {
                            lastResponseAt.set(completedAt);
                        }
                    }
                    return RespondentStatusDTO.builder()
                            .representativeId(rep.getId())
                            .name(rep.getUser().getFirstName() + " " + rep.getUser().getLastName())
                            .email(rep.getUser().getEmail())
                            .status(status)
                            .completedAt(completedAt)
                            .build();
                }).collect(Collectors.toList());

        int pending = Math.max(totalRespondents - responded.get(), 0);
        QuestionnaireResponseStatus progressStatus;
        if (responded.get() == 0) {
            progressStatus = QuestionnaireResponseStatus.PENDING;
        } else if (responded.get() < totalRespondents) {
            progressStatus = QuestionnaireResponseStatus.IN_PROGRESS;
        } else {
            progressStatus = QuestionnaireResponseStatus.COMPLETED;
        }

        return QuestionnaireSummaryResponseDTO.builder()
                .id(questionnaire.getId())
                .name(questionnaire.getName())
                .status(questionnaire.getStatus())
                .applicationStartDate(questionnaire.getApplicationStartDate())
                .applicationEndDate(questionnaire.getApplicationEndDate())
                .stageName(questionnaire.getStage() != null ? questionnaire.getStage().getName() : null)
                .iterationName(questionnaire.getIterationRef() != null ? questionnaire.getIterationRef().getName() : null)
                .totalRespondents(totalRespondents)
                .respondedRespondents(responded.get())
                .pendingRespondents(pending)
                .lastResponseAt(lastResponseAt.get())
                .progressStatus(progressStatus)
                .respondents(respondentStatus)
                .build();
    }

    @Override
    public User getCurrentUser() {
        return currentUserPort.getCurrentUser();
    }
}

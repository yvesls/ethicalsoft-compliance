package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectEditSnapshotDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.ProjectEditSnapshotDTO.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectIsepResultRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireResultRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentIterationPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentStagePolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectSituationPolicy;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetProjectEditSnapshotUseCase {

    private final ProjectRepository projectRepository;
    private final QuestionnaireResultRepository questionnaireResultRepository;
    private final ProjectIsepResultRepository projectIsepResultRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final ProjectCurrentStagePolicy projectCurrentStagePolicy;
    private final ProjectCurrentIterationPolicy projectCurrentIterationPolicy;
    private final ProjectSituationPolicy projectSituationPolicy;

    @Transactional(readOnly = true)
    public ProjectEditSnapshotDTO execute(Long projectId) {
        log.info("[edit-snapshot] Carregando snapshot de edição do projeto id={}", projectId);

        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado id=" + projectId));

        boolean hasProjectIsepResult = projectIsepResultRepository.existsByProjectId(projectId);

        boolean editable = true;
        String editableReason = null;
        if (hasProjectIsepResult) {
            editable = false;
            editableReason = "Projeto já possui resultado ISEP final calculado. Não é possível editar.";
        } else if (project.getStatus() == ProjectStatusEnum.CONCLUIDO) {
            editable = false;
            editableReason = "Projeto já foi concluído.";
        }

        Set<Integer> questionnairesWithResult = questionnaireResultRepository.findByProjectId(projectId)
                .stream()
                .map(QuestionnaireResult::getQuestionnaireId)
                .collect(Collectors.toSet());

        var allResponses = responseRepository.findByProjectId(projectId);
        Set<Long> representativesWithResponses = allResponses.stream()
                .map(QuestionnaireResponse::getRepresentativeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        String currentSituation = null;
        if (project.getType() == ProjectTypeEnum.CASCATA) {
            String currentStage = projectCurrentStagePolicy.findCurrentStageName(project.getStages(), LocalDate.now());
            currentSituation = projectSituationPolicy.buildCurrentSituation(project, currentStage, null);
        } else if (project.getType() == ProjectTypeEnum.ITERATIVO) {
            Integer currentIteration = projectCurrentIterationPolicy.findCurrentIterationNumber(project.getIterations(), LocalDate.now());
            currentSituation = projectSituationPolicy.buildCurrentSituation(project, null, currentIteration);
        }

        return ProjectEditSnapshotDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .type(project.getType() != null ? project.getType().name() : null)
                .startDate(project.getStartDate())
                .deadline(project.getDeadline())
                .closingDate(project.getClosingDate())
                .status(project.getStatus())
                .timelineStatus(project.getTimelineStatus())
                .iterationDuration(project.getIterationDuration())
                .iterationCount(project.getIterationCount())
                .currentSituation(currentSituation)
                .editable(editable)
                .editableReason(editableReason)
                .stages(buildStageSnapshots(project, questionnairesWithResult))
                .iterations(buildIterationSnapshots(project, questionnairesWithResult))
                .questionnaires(buildQuestionnaireSnapshots(project, questionnairesWithResult))
                .representatives(buildRepresentativeSnapshots(project, representativesWithResponses))
                .build();
    }

    private List<StageSnapshot> buildStageSnapshots(Project project, Set<Integer> questionnairesWithResult) {
        if (project.getStages() == null) return List.of();

        return project.getStages().stream()
                .sorted(Comparator.comparingInt(Stage::getSequence))
                .map(stage -> {
                    boolean hasActiveQuestionnaire = project.getQuestionnaires() != null &&
                            project.getQuestionnaires().stream()
                                    .anyMatch(q -> q.getStage() != null
                                            && Objects.equals(q.getStage().getId(), stage.getId())
                                            && questionnairesWithResult.contains(q.getId()));
                    boolean locked = hasActiveQuestionnaire;
                    String lockReason = locked ? "Etapa possui questionário(s) com resultado ISEP calculado." : null;

                    return StageSnapshot.builder()
                            .id(stage.getId())
                            .name(stage.getName())
                            .weight(stage.getWeight())
                            .sequence(stage.getSequence())
                            .durationDays(stage.getDurationDays())
                            .applicationStartDate(stage.getApplicationStartDate())
                            .applicationEndDate(stage.getApplicationEndDate())
                            .status(stage.getStatus())
                            .locked(locked)
                            .lockReason(lockReason)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<IterationSnapshot> buildIterationSnapshots(Project project, Set<Integer> questionnairesWithResult) {
        if (project.getIterations() == null) return List.of();

        return project.getIterations().stream()
                .sorted(Comparator.comparing(Iteration::getApplicationStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(iter -> {
                    boolean hasActiveQuestionnaire = project.getQuestionnaires() != null &&
                            project.getQuestionnaires().stream()
                                    .anyMatch(q -> q.getIterationRef() != null
                                            && Objects.equals(q.getIterationRef().getId(), iter.getId())
                                            && questionnairesWithResult.contains(q.getId()));
                    String lockReason = hasActiveQuestionnaire ? "Iteração possui questionário(s) com resultado ISEP calculado." : null;

                    return IterationSnapshot.builder()
                            .id(iter.getId())
                            .name(iter.getName())
                            .weight(iter.getWeight())
                            .applicationStartDate(iter.getApplicationStartDate())
                            .applicationEndDate(iter.getApplicationEndDate())
                            .status(iter.getStatus())
                            .locked(hasActiveQuestionnaire)
                            .lockReason(lockReason)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<QuestionnaireSnapshot> buildQuestionnaireSnapshots(Project project, Set<Integer> questionnairesWithResult) {
        if (project.getQuestionnaires() == null) return List.of();

        return project.getQuestionnaires().stream()
                .sorted(Comparator.comparing(Questionnaire::getId))
                .map(q -> {
                    boolean hasResult = questionnairesWithResult.contains(q.getId());
                    boolean locked = hasResult;
                    String lockReason = locked ? "Questionário já possui resultado ISEP calculado." : null;

                    return QuestionnaireSnapshot.builder()
                            .id(q.getId())
                            .name(q.getName())
                            .weight(q.getWeight())
                            .stageName(q.getStage() != null ? q.getStage().getName() : null)
                            .stageId(q.getStage() != null ? q.getStage().getId() : null)
                            .iterationName(q.getIterationRef() != null ? q.getIterationRef().getName() : null)
                            .iterationId(q.getIterationRef() != null ? q.getIterationRef().getId() : null)
                            .applicationStartDate(q.getApplicationStartDate())
                            .applicationEndDate(q.getApplicationEndDate())
                            .status(q.getStatus())
                            .domain(q.getDomain())
                            .description(q.getDescription())
                            .hasIsepResult(hasResult)
                            .locked(locked)
                            .lockReason(lockReason)
                            .questions(buildQuestionSnapshots(q))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<QuestionSnapshot> buildQuestionSnapshots(Questionnaire q) {
        if (q.getQuestions() == null) return List.of();

        return q.getQuestions().stream()
                .sorted(Comparator.comparing(Question::getId))
                .map(question -> QuestionSnapshot.builder()
                        .id(question.getId())
                        .text(question.getValue())
                        .roleIds(question.getRoles() != null
                                ? question.getRoles().stream().map(Role::getId).collect(Collectors.toSet())
                                : Set.of())
                        .roleNames(question.getRoles() != null
                                ? question.getRoles().stream().map(Role::getDescription).sorted().collect(Collectors.toList())
                                : List.of())
                        .stageIds(question.getStages() != null
                                ? question.getStages().stream().map(Stage::getId).sorted().collect(Collectors.toList())
                                : List.of())
                        .stageNames(question.getStages() != null
                                ? question.getStages().stream().map(Stage::getName).sorted().collect(Collectors.toList())
                                : List.of())
                        .build())
                .collect(Collectors.toList());
    }

    private List<RepresentativeSnapshot> buildRepresentativeSnapshots(Project project, Set<Long> representativesWithResponses) {
        if (project.getRepresentatives() == null) return List.of();

        return project.getRepresentatives().stream()
                .sorted(Comparator.comparing(Representative::getId))
                .map(rep -> {
                    boolean hasResponses = representativesWithResponses.contains(rep.getId());
                    String lockReason = hasResponses ? "Representante já possui respostas submetidas. Remoção não permitida." : null;

                    return RepresentativeSnapshot.builder()
                            .id(rep.getId())
                            .userId(rep.getUser() != null ? rep.getUser().getId() : null)
                            .firstName(rep.getUser() != null ? rep.getUser().getFirstName() : null)
                            .lastName(rep.getUser() != null ? rep.getUser().getLastName() : null)
                            .email(rep.getUser() != null ? rep.getUser().getEmail() : null)
                            .roleIds(rep.getRoles() != null
                                    ? rep.getRoles().stream().map(Role::getId).collect(Collectors.toSet())
                                    : Set.of())
                            .roleNames(rep.getRoles() != null
                                    ? rep.getRoles().stream().map(Role::getDescription).sorted().collect(Collectors.toList())
                                    : List.of())
                            .weight(rep.getWeight())
                            .hasResponses(hasResponses)
                            .locked(hasResponses)
                            .lockReason(lockReason)
                            .build();
                })
                .collect(Collectors.toList());
    }
}


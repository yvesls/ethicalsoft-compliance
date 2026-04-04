package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.UpdateProjectRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.UpdateProjectRequestDTO.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.UpdateProjectResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.UpdateProjectResponseDTO.ChangesSummaryDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.*;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.service.ProjectUpdateResponseSyncService;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectTimelineStatusPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectUpdateValidationPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectUpdateValidationPolicy.ValidationResult;
import com.ethicalsoft.ethicalsoft_complience.domain.service.UserResolutionPolicy;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateProjectUseCase {

    private final ProjectRepository projectRepository;
    private final StageRepository stageRepository;
    private final IterationRepository iterationRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final RepresentativeRepository representativeRepository;
    private final RoleRepository roleRepository;
    private final QuestionnaireResultRepository questionnaireResultRepository;
    private final ProjectIsepResultRepository projectIsepResultRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final ProjectUpdateValidationPolicy validationPolicy;
    private final ProjectUpdateResponseSyncService responseSyncService;
    private final ProjectTimelineStatusPolicy timelineStatusPolicy;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final CurrentUserPort currentUserPort;
    private final UserResolutionPolicy userResolutionPolicy;

    @Transactional
    public UpdateProjectResponseDTO execute(Long projectId, UpdateProjectRequestDTO request) {
        log.info("[update-project] Iniciando atualização do projeto id={} dryRun={}", projectId, request.isDryRun());

        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado id=" + projectId));

        boolean hasProjectIsepResult = projectIsepResultRepository.existsByProjectId(projectId);
        Map<Integer, Boolean> questionnaireHasResult = buildQuestionnaireResultMap(projectId);
        Map<Integer, List<QuestionnaireResponse>> responsesByQuestionnaire = buildResponsesByQuestionnaireMap(projectId);

        ValidationResult validation = validationPolicy.validate(project, hasProjectIsepResult, questionnaireHasResult, responsesByQuestionnaire);
        if (validation.isBlocked()) {
            throw new BusinessException(validation.blocked());
        }

        int[] counters = new int[15];
        int responsesCreated = 0, responsesUpdated = 0, responsesDeleted = 0, notificationsSent = 0;
        List<String> warnings = new ArrayList<>(validation.warnings());
        List<String> allBlocked = new ArrayList<>();

        updateProjectScalars(project, request);

        counters[0] = counters[1] = counters[2] = 0;
        if (request.getStages() != null) {
            int[] stageCounters = processStages(project, request.getStages(), questionnaireHasResult, responsesByQuestionnaire, allBlocked, warnings);
            counters[0] = stageCounters[0]; counters[1] = stageCounters[1]; counters[2] = stageCounters[2];
        }

        if (request.getIterations() != null) {
            int[] iterCounters = processIterations(project, request.getIterations(), questionnaireHasResult, responsesByQuestionnaire, allBlocked, warnings);
            counters[3] = iterCounters[0]; counters[4] = iterCounters[1]; counters[5] = iterCounters[2];
        }

        if (request.getQuestionnaires() != null) {
            int[] qCounters = processQuestionnaires(project, request.getQuestionnaires(), questionnaireHasResult, responsesByQuestionnaire, allBlocked, warnings);
            counters[6] = qCounters[0]; counters[7] = qCounters[1]; counters[8] = qCounters[2];
            counters[9] = qCounters[3]; counters[10] = qCounters[4]; counters[11] = qCounters[5];
            responsesUpdated += qCounters[6];
            responsesDeleted += qCounters[7];
        }

        if (request.getRepresentatives() != null) {
            int[] repResult = processRepresentatives(project, request.getRepresentatives(), questionnaireHasResult, responsesByQuestionnaire, allBlocked, warnings);
            counters[12] = repResult[0]; counters[13] = repResult[1]; counters[14] = repResult[2];
            responsesCreated += repResult[3];
            responsesDeleted += repResult[4];
            notificationsSent += repResult[5];
        }

        if (!allBlocked.isEmpty()) {
            throw new BusinessException(allBlocked);
        }

        if (request.isDryRun()) {
            return buildResponse(project, counters, responsesCreated, responsesUpdated, responsesDeleted, notificationsSent, warnings, allBlocked);
        }

        timelineStatusPolicy.updateProjectTimeline(project);
        projectRepository.save(project);

        log.info("[update-project] Projeto id={} atualizado com sucesso", projectId);

        return buildResponse(project, counters, responsesCreated, responsesUpdated, responsesDeleted, notificationsSent, warnings, allBlocked);
    }

    private void updateProjectScalars(Project project, UpdateProjectRequestDTO request) {
        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getStartDate() != null) {
            project.setStartDate(request.getStartDate());
        }
        if (request.getDeadline() != null) {
            if (request.getDeadline().isBefore(LocalDate.now())) {
                throw new BusinessException("Deadline não pode ser anterior a hoje.");
            }
            project.setDeadline(request.getDeadline());
        }
        if (request.getIterationDuration() != null) {
            project.setIterationDuration(request.getIterationDuration());
        }
        if (request.getIterationCount() != null) {
            project.setIterationCount(request.getIterationCount());
        }
        projectRepository.save(project);
    }

    private int[] processStages(Project project, List<UpdateStageDTO> requestStages,
                                Map<Integer, Boolean> qHasResult,
                                Map<Integer, List<QuestionnaireResponse>> responsesByQ,
                                List<String> blocked, List<String> warnings) {
        int added = 0, removed = 0, updated = 0;

        Map<Integer, Stage> existingById = project.getStages() != null
                ? project.getStages().stream().filter(s -> s.getId() != null).collect(Collectors.toMap(Stage::getId, Function.identity()))
                : new HashMap<>();

        Set<Integer> requestIds = requestStages.stream()
                .filter(s -> s.getId() != null).map(UpdateStageDTO::getId).collect(Collectors.toSet());

        for (Stage existing : new ArrayList<>(existingById.values())) {
            if (!requestIds.contains(existing.getId())) {
                boolean hasActiveQuestionnaires = project.getQuestionnaires() != null &&
                        project.getQuestionnaires().stream()
                                .anyMatch(q -> q.getStage() != null && Objects.equals(q.getStage().getId(), existing.getId())
                                        && q.getStatus() != TimelineStatusEnum.PENDENTE);
                if (hasActiveQuestionnaires) {
                    blocked.add("Não é possível remover etapa '" + existing.getName() + "' com questionários ativos.");
                    continue;
                }
                stageRepository.deleteById(Long.valueOf(existing.getId()));
                removed++;
            }
        }

        int sequence = existingById.size() + 1;
        for (UpdateStageDTO dto : requestStages) {
            if (dto.getId() == null) {
                Stage newStage = new Stage();
                newStage.setName(dto.getName());
                newStage.setWeight(dto.getWeight());
                newStage.setProject(project);
                newStage.setSequence(sequence++);
                newStage.setDurationDays(dto.getDurationDays());
                newStage.setApplicationStartDate(dto.getApplicationStartDate());
                newStage.setApplicationEndDate(dto.getApplicationEndDate());
                newStage.setStatus(TimelineStatusEnum.PENDENTE);
                stageRepository.save(newStage);
                added++;
            } else {
                Stage existing = existingById.get(dto.getId());
                if (existing != null) {
                    boolean changed = false;
                    if (dto.getName() != null && !dto.getName().equals(existing.getName())) {
                        existing.setName(dto.getName());
                        changed = true;
                    }
                    if (dto.getWeight() != null && dto.getWeight().compareTo(existing.getWeight()) != 0) {
                        existing.setWeight(dto.getWeight());
                        changed = true;
                    }
                    if (dto.getApplicationStartDate() != null && !dto.getApplicationStartDate().equals(existing.getApplicationStartDate())) {
                        existing.setApplicationStartDate(dto.getApplicationStartDate());
                        changed = true;
                    }
                    if (dto.getApplicationEndDate() != null && !dto.getApplicationEndDate().equals(existing.getApplicationEndDate())) {
                        existing.setApplicationEndDate(dto.getApplicationEndDate());
                        changed = true;
                    }
                    if (dto.getDurationDays() != null && !dto.getDurationDays().equals(existing.getDurationDays())) {
                        existing.setDurationDays(dto.getDurationDays());
                        changed = true;
                    }
                    if (changed) {
                        stageRepository.save(existing);
                        updated++;
                    }
                }
            }
        }

        return new int[]{added, removed, updated};
    }

    private int[] processIterations(Project project, Set<UpdateIterationDTO> requestIterations,
                                    Map<Integer, Boolean> qHasResult,
                                    Map<Integer, List<QuestionnaireResponse>> responsesByQ,
                                    List<String> blocked, List<String> warnings) {
        int added = 0, removed = 0, updated = 0;

        Map<Integer, Iteration> existingById = project.getIterations() != null
                ? project.getIterations().stream().filter(i -> i.getId() != null).collect(Collectors.toMap(Iteration::getId, Function.identity()))
                : new HashMap<>();

        Set<Integer> requestIds = requestIterations.stream()
                .filter(i -> i.getId() != null).map(UpdateIterationDTO::getId).collect(Collectors.toSet());

        for (Iteration existing : new ArrayList<>(existingById.values())) {
            if (!requestIds.contains(existing.getId())) {
                boolean hasActiveQuestionnaires = project.getQuestionnaires() != null &&
                        project.getQuestionnaires().stream()
                                .anyMatch(q -> q.getIterationRef() != null && Objects.equals(q.getIterationRef().getId(), existing.getId())
                                        && q.getStatus() != TimelineStatusEnum.PENDENTE);
                if (hasActiveQuestionnaires) {
                    blocked.add("Não é possível remover iteração '" + existing.getName() + "' com questionários ativos.");
                    continue;
                }
                iterationRepository.deleteById(Long.valueOf(existing.getId()));
                removed++;
            }
        }

        for (UpdateIterationDTO dto : requestIterations) {
            if (dto.getId() == null) {
                Iteration newIter = new Iteration();
                newIter.setName(dto.getName());
                newIter.setWeight(dto.getWeight());
                newIter.setProject(project);
                newIter.setApplicationStartDate(dto.getApplicationStartDate());
                newIter.setApplicationEndDate(dto.getApplicationEndDate());
                newIter.setStatus(TimelineStatusEnum.PENDENTE);
                iterationRepository.save(newIter);
                added++;
            } else {
                Iteration existing = existingById.get(dto.getId());
                if (existing != null) {
                    boolean changed = false;
                    if (dto.getName() != null && !dto.getName().equals(existing.getName())) {
                        existing.setName(dto.getName());
                        changed = true;
                    }
                    if (dto.getWeight() != null && dto.getWeight().compareTo(existing.getWeight()) != 0) {
                        existing.setWeight(dto.getWeight());
                        changed = true;
                    }
                    if (dto.getApplicationStartDate() != null) {
                        existing.setApplicationStartDate(dto.getApplicationStartDate());
                        changed = true;
                    }
                    if (dto.getApplicationEndDate() != null) {
                        existing.setApplicationEndDate(dto.getApplicationEndDate());
                        changed = true;
                    }
                    if (changed) {
                        iterationRepository.save(existing);
                        updated++;
                    }
                }
            }
        }

        return new int[]{added, removed, updated};
    }

    private int[] processQuestionnaires(Project project, Set<UpdateQuestionnaireDTO> requestQuestionnaires,
                                        Map<Integer, Boolean> qHasResult,
                                        Map<Integer, List<QuestionnaireResponse>> responsesByQ,
                                        List<String> blocked, List<String> warnings) {
        int qAdded = 0, qRemoved = 0, qUpdated = 0;
        int questAdded = 0, questRemoved = 0, questUpdated = 0;
        int respUpdated = 0, respDeleted = 0;

        Map<Integer, Questionnaire> existingById = project.getQuestionnaires() != null
                ? project.getQuestionnaires().stream().filter(q -> q.getId() != null).collect(Collectors.toMap(Questionnaire::getId, Function.identity()))
                : new HashMap<>();

        Set<Integer> requestIds = requestQuestionnaires.stream()
                .filter(q -> q.getId() != null).map(UpdateQuestionnaireDTO::getId).collect(Collectors.toSet());

        for (Questionnaire existing : new ArrayList<>(existingById.values())) {
            if (!requestIds.contains(existing.getId())) {
                List<String> blockReasons = validationPolicy.validateQuestionnaireRemoval(
                        existing, qHasResult.getOrDefault(existing.getId(), false),
                        responsesByQ.getOrDefault(existing.getId(), List.of()));
                if (!blockReasons.isEmpty()) {
                    blocked.addAll(blockReasons);
                    continue;
                }
                respDeleted += responseSyncService.deleteResponsesForQuestionnaire(project.getId(), existing.getId());
                if (existing.getQuestions() != null) {
                    existing.getQuestions().forEach(q -> questionRepository.deleteById(Long.valueOf(q.getId())));
                }
                questionnaireRepository.deleteById(existing.getId());
                qRemoved++;
            }
        }

        for (UpdateQuestionnaireDTO dto : requestQuestionnaires) {
            if (dto.getId() == null) {
                Questionnaire newQ = createQuestionnaireFromDTO(project, dto);
                questionnaireRepository.save(newQ);
                if (dto.getQuestions() != null) {
                    for (UpdateQuestionDTO qDto : dto.getQuestions()) {
                        createQuestionFromDTO(newQ, qDto, project);
                        questAdded++;
                    }
                }
                createResponsesForNewQuestionnaire(project, newQ);
                qAdded++;
            } else {
                Questionnaire existing = existingById.get(dto.getId());
                if (existing == null) continue;

                boolean hasResult = qHasResult.getOrDefault(existing.getId(), false);
                List<QuestionnaireResponse> qResponses = responsesByQ.getOrDefault(existing.getId(), List.of());

                boolean hasNameChange = dto.getName() != null && !dto.getName().equals(existing.getName());
                boolean hasWeightChange = dto.getWeight() != null && !dto.getWeight().equals(existing.getWeight());
                boolean hasDomainChange = dto.getDomain() != null && !dto.getDomain().equals(existing.getDomain());
                boolean hasDescriptionChange = dto.getDescription() != null && !dto.getDescription().equals(existing.getDescription());
                boolean hasEndDateChange = dto.getApplicationEndDate() != null
                        && !dto.getApplicationEndDate().equals(existing.getApplicationEndDate());
                boolean hasStartDateChange = dto.getApplicationStartDate() != null
                        && !dto.getApplicationStartDate().equals(existing.getApplicationStartDate());

                boolean hasScalarChanges = hasNameChange || hasWeightChange || hasDomainChange
                        || hasDescriptionChange || hasEndDateChange || hasStartDateChange;

                boolean hasQuestionChanges = false;
                if (dto.getQuestions() != null && existing.getQuestions() != null) {
                    Set<Integer> existingQuestionIds = existing.getQuestions().stream()
                            .filter(q -> q.getId() != null).map(Question::getId).collect(Collectors.toSet());
                    Set<Integer> requestQuestionIds = dto.getQuestions().stream()
                            .filter(q -> q.getId() != null).map(UpdateQuestionDTO::getId).collect(Collectors.toSet());

                    boolean hasNewQuestions = dto.getQuestions().stream().anyMatch(q -> q.getId() == null);
                    boolean hasRemovedQuestions = !requestQuestionIds.containsAll(existingQuestionIds);
                    boolean hasEditedQuestions = dto.getQuestions().stream()
                            .filter(q -> q.getId() != null)
                            .anyMatch(q -> {
                                Question eq = existing.getQuestions().stream()
                                        .filter(ex -> Objects.equals(ex.getId(), q.getId())).findFirst().orElse(null);
                                if (eq == null) return false;
                                boolean textChanged = q.getValue() != null && !q.getValue().equals(eq.getValue());
                                boolean roleChanged = q.getRoleIds() != null && !q.getRoleIds().equals(
                                        eq.getRoles().stream().map(Role::getId).collect(Collectors.toSet()));
                                return textChanged || roleChanged;
                            });

                    hasQuestionChanges = hasNewQuestions || hasRemovedQuestions || hasEditedQuestions;
                } else if (dto.getQuestions() != null && existing.getQuestions() == null) {
                    hasQuestionChanges = !dto.getQuestions().isEmpty();
                }

                if (!hasScalarChanges && !hasQuestionChanges) {
                    continue;
                }

                boolean qChanged = false;
                boolean isStructuralChange = hasWeightChange;

                List<String> updateBlocked = validationPolicy.validateQuestionnaireUpdate(
                        existing, hasResult, qResponses, isStructuralChange);
                if (!updateBlocked.isEmpty()) {
                    blocked.addAll(updateBlocked);
                } else {
                    if (hasNameChange) {
                        existing.setName(dto.getName());
                        qChanged = true;
                    }
                    if (hasWeightChange) {
                        existing.setWeight(dto.getWeight());
                        qChanged = true;
                    }
                    if (hasDomainChange) {
                        existing.setDomain(dto.getDomain());
                        qChanged = true;
                    }
                    if (hasDescriptionChange) {
                        existing.setDescription(dto.getDescription());
                        qChanged = true;
                    }

                    if (hasEndDateChange) {
                        List<String> dateBlocked = validationPolicy.validateDatesChange(
                                existing, dto.getApplicationEndDate(), qResponses);
                        if (!dateBlocked.isEmpty()) {
                            blocked.addAll(dateBlocked);
                        } else {
                            existing.setApplicationEndDate(dto.getApplicationEndDate());
                            qChanged = true;
                        }
                    }
                    if (hasStartDateChange) {
                        existing.setApplicationStartDate(dto.getApplicationStartDate());
                        qChanged = true;
                    }

                    if (qChanged) {
                        questionnaireRepository.save(existing);
                        qUpdated++;
                    }

                    if (dto.getQuestions() != null && hasQuestionChanges) {
                        int[] questResult = processQuestions(existing, dto.getQuestions(), project,
                                qResponses, blocked, warnings);
                        questAdded += questResult[0];
                        questRemoved += questResult[1];
                        questUpdated += questResult[2];
                        respUpdated += questResult[3];
                    }
                }
            }
        }

        return new int[]{qAdded, qRemoved, qUpdated, questAdded, questRemoved, questUpdated, respUpdated, respDeleted};
    }

    private int[] processQuestions(Questionnaire questionnaire, Set<UpdateQuestionDTO> requestQuestions,
                                   Project project, List<QuestionnaireResponse> responses,
                                   List<String> blocked, List<String> warnings) {
        int added = 0, removed = 0, updated = 0, respUpdated = 0;

        Map<Integer, Question> existingById = questionnaire.getQuestions() != null
                ? questionnaire.getQuestions().stream().filter(q -> q.getId() != null).collect(Collectors.toMap(Question::getId, Function.identity()))
                : new HashMap<>();

        Set<Integer> requestIds = requestQuestions.stream()
                .filter(q -> q.getId() != null).map(UpdateQuestionDTO::getId).collect(Collectors.toSet());

        for (Question existing : new ArrayList<>(existingById.values())) {
            if (!requestIds.contains(existing.getId())) {
                List<String> blockReasons = validationPolicy.validateQuestionRemoval(existing, questionnaire, responses);
                if (!blockReasons.isEmpty()) {
                    blocked.addAll(blockReasons);
                    continue;
                }
                respUpdated += responseSyncService.removeQuestionFromResponses(questionnaire.getId(), existing.getId());
                questionRepository.deleteById(Long.valueOf(existing.getId()));
                removed++;
            }
        }

        for (UpdateQuestionDTO dto : requestQuestions) {
            if (dto.getId() == null) {
                List<String> addBlocked = validationPolicy.validateQuestionAddition(questionnaire, responses);
                if (!addBlocked.isEmpty()) {
                    blocked.addAll(addBlocked);
                    continue;
                }
                Question newQ = createQuestionFromDTO(questionnaire, dto, project);
                Set<Representative> reps = project.getRepresentatives() != null ? project.getRepresentatives() : Set.of();
                respUpdated += responseSyncService.addQuestionToResponses(questionnaire.getId(), newQ, reps);
                added++;
            } else {
                Question existing = existingById.get(dto.getId());
                if (existing == null) continue;

                boolean isTextChange = dto.getValue() != null && !dto.getValue().equals(existing.getValue());
                boolean isRoleChange = dto.getRoleIds() != null;

                List<String> editBlocked = validationPolicy.validateQuestionUpdate(
                        existing, questionnaire, responses, isTextChange, isRoleChange);
                if (!editBlocked.isEmpty()) {
                    blocked.addAll(editBlocked);
                    continue;
                }

                boolean changed = false;

                if (isTextChange) {
                    existing.setValue(dto.getValue());
                    responseSyncService.updateQuestionTextInResponses(questionnaire.getId(), existing.getId(), dto.getValue());
                    changed = true;
                }

                if (dto.getRoleIds() != null) {
                    Set<Long> existingRoleIds = existing.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
                    if (!existingRoleIds.equals(dto.getRoleIds())) {
                        Set<Role> newRoles = new HashSet<>(roleRepository.findAllById(dto.getRoleIds()));
                        existing.setRoles(newRoles);
                        changed = true;
                    }
                }

                if (dto.getStageNames() != null) {
                    Set<Stage> projectStages = project.getStages() != null ? project.getStages() : Set.of();
                    Set<Stage> newStages = dto.getStageNames().stream()
                            .map(name -> projectStages.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst().orElse(null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    existing.setStages(newStages);
                    changed = true;
                }

                if (changed) {
                    questionRepository.save(existing);
                    updated++;
                }
            }
        }

        return new int[]{added, removed, updated, respUpdated};
    }

    private int[] processRepresentatives(Project project, Set<UpdateRepresentativeDTO> requestReps,
                                         Map<Integer, Boolean> qHasResult,
                                         Map<Integer, List<QuestionnaireResponse>> responsesByQ,
                                         List<String> blocked, List<String> warnings) {
        int added = 0, removed = 0, updated = 0;
        int respCreated = 0, respDeleted = 0, notifSent = 0;

        Map<Long, Representative> existingById = project.getRepresentatives() != null
                ? project.getRepresentatives().stream().filter(r -> r.getId() != null).collect(Collectors.toMap(Representative::getId, Function.identity()))
                : new HashMap<>();

        Set<Long> requestIds = requestReps.stream()
                .filter(r -> r.getId() != null).map(UpdateRepresentativeDTO::getId).collect(Collectors.toSet());

        for (Representative existing : new ArrayList<>(existingById.values())) {
            if (!requestIds.contains(existing.getId())) {
                List<String> blockReasons = validationPolicy.validateRepresentativeRemoval(existing, responsesByQ, qHasResult);
                if (!blockReasons.isEmpty()) {
                    blocked.addAll(blockReasons);
                    continue;
                }
                existing.setDeletionDate(LocalDate.now());
                representativeRepository.save(existing);
                respDeleted += responseSyncService.deleteResponsesForRemovedRepresentative(project.getId(), existing.getId());
                sendRemovalNotification(existing, project);
                notifSent++;
                removed++;
            }
        }

        User currentAdmin = currentUserPort.getCurrentUser();
        for (UpdateRepresentativeDTO dto : requestReps) {
            if (dto.getId() == null) {
                Representative newRep = createRepresentativeFromDTO(project, dto, currentAdmin);
                respCreated += responseSyncService.createResponsesForNewRepresentative(project, newRep);
                sendAssignmentNotification(newRep, project, currentAdmin);
                notifSent++;
                added++;
            } else {
                Representative existing = existingById.get(dto.getId());
                if (existing == null) continue;

                boolean changed = false;

                if (dto.getWeight() != null && dto.getWeight().compareTo(existing.getWeight()) != 0) {
                    existing.setWeight(dto.getWeight());
                    changed = true;
                }

                if (dto.getRoleIds() != null) {
                    Set<Long> existingRoleIds = existing.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
                    if (!existingRoleIds.equals(dto.getRoleIds())) {
                        Set<Role> newRoles = new HashSet<>(roleRepository.findAllById(dto.getRoleIds()));
                        existing.setRoles(newRoles);
                        changed = true;
                    }
                }

                if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(existing.getUser().getEmail())) {
                    String oldEmail = existing.getUser().getEmail();
                    RepresentativeDTO repDTO = new RepresentativeDTO();
                    repDTO.setEmail(dto.getEmail());
                    repDTO.setFirstName(dto.getFirstName() != null ? dto.getFirstName() : existing.getUser().getFirstName());
                    repDTO.setLastName(dto.getLastName() != null ? dto.getLastName() : existing.getUser().getLastName());
                    UserResolutionPolicy.UserResolutionResult resolution = userResolutionPolicy.resolveOrCreateUser(repDTO);
                    existing.setUser(resolution.user());
                    changed = true;

                    sendEmailChangedNotification(oldEmail, dto.getEmail(), project);
                    notifSent += 2;

                    resolution.temporaryPassword().ifPresent(tempPwd -> {
                        try {
                            sendNotificationUseCase.execute(new SendNotificationCommand(
                                    NotificationType.NEW_USER_CREDENTIALS,
                                    Map.of(
                                            "to", dto.getEmail(),
                                            "firstName", repDTO.getFirstName(),
                                            "tempPassword", tempPwd,
                                            "projectName", project.getName(),
                                            "adminName", currentAdmin.getFirstName() + " " + currentAdmin.getLastName(),
                                            "projectId", project.getId()
                                    )
                            ));
                        } catch (Exception e) {
                            log.warn("[update-project] Falha ao enviar credenciais para {}", dto.getEmail(), e);
                        }
                    });
                }

                if (changed) {
                    existing.setUpdateDate(LocalDate.now());
                    representativeRepository.save(existing);
                    updated++;
                }
            }
        }

        return new int[]{added, removed, updated, respCreated, respDeleted, notifSent};
    }

    private Questionnaire createQuestionnaireFromDTO(Project project, UpdateQuestionnaireDTO dto) {
        Questionnaire q = new Questionnaire();
        q.setName(dto.getName());
        q.setWeight(dto.getWeight() != null ? dto.getWeight() : 1);
        q.setProject(project);
        q.setApplicationStartDate(dto.getApplicationStartDate());
        q.setApplicationEndDate(dto.getApplicationEndDate());
        q.setStatus(TimelineStatusEnum.PENDENTE);
        q.setDomain(dto.getDomain());
        q.setDescription(dto.getDescription());

        if (dto.getStageName() != null && project.getStages() != null) {
            project.getStages().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(dto.getStageName()))
                    .findFirst()
                    .ifPresent(q::setStage);
        }
        if (dto.getIterationName() != null && project.getIterations() != null) {
            project.getIterations().stream()
                    .filter(i -> i.getName().equalsIgnoreCase(dto.getIterationName()))
                    .findFirst()
                    .ifPresent(q::setIterationRef);
        }

        return questionnaireRepository.save(q);
    }

    private Question createQuestionFromDTO(Questionnaire questionnaire, UpdateQuestionDTO dto, Project project) {
        Question q = new Question();
        q.setValue(dto.getValue());
        q.setQuestionnaire(questionnaire);

        if (dto.getRoleIds() != null) {
            q.setRoles(new HashSet<>(roleRepository.findAllById(dto.getRoleIds())));
        }

        if (dto.getStageNames() != null && project.getStages() != null) {
            Set<Stage> stages = dto.getStageNames().stream()
                    .map(name -> project.getStages().stream()
                            .filter(s -> s.getName().equalsIgnoreCase(name))
                            .findFirst().orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            q.setStages(stages);
        }

        return questionRepository.save(q);
    }

    private Representative createRepresentativeFromDTO(Project project, UpdateRepresentativeDTO dto, User currentAdmin) {
        RepresentativeDTO repDTO = new RepresentativeDTO();
        repDTO.setEmail(dto.getEmail());
        repDTO.setFirstName(dto.getFirstName());
        repDTO.setLastName(dto.getLastName());
        repDTO.setRoleIds(dto.getRoleIds());
        repDTO.setWeight(dto.getWeight());

        UserResolutionPolicy.UserResolutionResult resolution = userResolutionPolicy.resolveOrCreateUser(repDTO);

        Representative rep = new Representative();
        rep.setProject(project);
        rep.setUser(resolution.user());
        rep.setWeight(dto.getWeight());
        rep.setCreationDate(LocalDate.now());

        if (dto.getRoleIds() != null) {
            rep.setRoles(new HashSet<>(roleRepository.findAllById(dto.getRoleIds())));
        }

        representativeRepository.save(rep);

        resolution.temporaryPassword().ifPresent(tempPwd -> {
            try {
                sendNotificationUseCase.execute(new SendNotificationCommand(
                        NotificationType.NEW_USER_CREDENTIALS,
                        Map.of(
                                "to", dto.getEmail(),
                                "firstName", dto.getFirstName() != null ? dto.getFirstName() : "",
                                "tempPassword", tempPwd,
                                "projectName", project.getName(),
                                "adminName", currentAdmin.getFirstName() + " " + currentAdmin.getLastName(),
                                "projectId", project.getId()
                        )
                ));
            } catch (Exception e) {
                log.warn("[update-project] Falha ao enviar credenciais para {}", dto.getEmail(), e);
            }
        });

        return rep;
    }

    private void createResponsesForNewQuestionnaire(Project project, Questionnaire questionnaire) {
        if (project.getRepresentatives() == null) return;

        Questionnaire reloaded = questionnaireRepository.findAllByProjectIdWithQuestions(project.getId()).stream()
                .filter(q -> Objects.equals(q.getId(), questionnaire.getId()))
                .findFirst().orElse(questionnaire);

        for (Representative rep : project.getRepresentatives()) {
            if (rep.getDeletionDate() != null) continue;
            responseSyncService.createResponsesForNewRepresentative(project, rep);
        }
    }

    private void sendAssignmentNotification(Representative rep, Project project, User admin) {
        try {
            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.PROJECT_ASSIGNMENT,
                    Map.of(
                            "to", rep.getUser().getEmail(),
                            "firstName", Optional.ofNullable(rep.getUser().getFirstName()).orElse(""),
                            "projectName", project.getName(),
                            "projectId", project.getId(),
                            "adminName", admin.getFirstName() + " " + Optional.ofNullable(admin.getLastName()).orElse(""),
                            "adminEmail", Optional.ofNullable(admin.getEmail()).orElse(""),
                            "roles", rep.getRoles().stream().map(Role::getName).toList(),
                            "startDate", project.getStartDate(),
                            "deadline", project.getDeadline()
                    )
            ));
        } catch (Exception e) {
            log.warn("[update-project] Falha ao enviar notificação de atribuição para {}", rep.getUser().getEmail(), e);
        }
    }

    private void sendRemovalNotification(Representative rep, Project project) {
        try {
            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.PROJECT_UNASSIGNMENT,
                    Map.of(
                            "to", rep.getUser().getEmail(),
                            "firstName", Optional.ofNullable(rep.getUser().getFirstName()).orElse(""),
                            "projectName", project.getName(),
                            "projectId", project.getId()
                    )
            ));
        } catch (Exception e) {
            log.warn("[update-project] Falha ao enviar notificação de remoção para {}", rep.getUser().getEmail(), e);
        }
    }

    private void sendEmailChangedNotification(String oldEmail, String newEmail, Project project) {
        try {
            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.REPRESENTATIVE_EMAIL_CHANGED,
                    Map.of(
                            "to", oldEmail,
                            "oldEmail", oldEmail,
                            "newEmail", newEmail,
                            "projectName", project.getName(),
                            "projectId", project.getId()
                    )
            ));
            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.REPRESENTATIVE_EMAIL_CHANGED,
                    Map.of(
                            "to", newEmail,
                            "oldEmail", oldEmail,
                            "newEmail", newEmail,
                            "projectName", project.getName(),
                            "projectId", project.getId()
                    )
            ));
        } catch (Exception e) {
            log.warn("[update-project] Falha ao enviar notificação de mudança de email {} -> {}", oldEmail, newEmail, e);
        }
    }

    private Map<Integer, Boolean> buildQuestionnaireResultMap(Long projectId) {
        List<QuestionnaireResult> results = questionnaireResultRepository.findByProjectId(projectId);
        return results.stream().collect(Collectors.toMap(QuestionnaireResult::getQuestionnaireId, r -> true, (a, b) -> a));
    }

    private Map<Integer, List<QuestionnaireResponse>> buildResponsesByQuestionnaireMap(Long projectId) {
        List<QuestionnaireResponse> all = responseRepository.findByProjectId(projectId);
        return all.stream()
                .filter(r -> r.getRepresentativeId() != null)
                .collect(Collectors.groupingBy(QuestionnaireResponse::getQuestionnaireId));
    }

    private UpdateProjectResponseDTO buildResponse(Project project, int[] counters,
                                                    int responsesCreated, int responsesUpdated, int responsesDeleted,
                                                    int notificationsSent,
                                                    List<String> warnings, List<String> blockedReasons) {
        return UpdateProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .type(project.getType().name())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .deadline(project.getDeadline())
                .timelineStatus(project.getTimelineStatus())
                .changesSummary(ChangesSummaryDTO.builder()
                        .stagesAdded(counters[0]).stagesRemoved(counters[1]).stagesUpdated(counters[2])
                        .iterationsAdded(counters[3]).iterationsRemoved(counters[4]).iterationsUpdated(counters[5])
                        .questionnairesAdded(counters[6]).questionnairesRemoved(counters[7]).questionnairesUpdated(counters[8])
                        .questionsAdded(counters[9]).questionsRemoved(counters[10]).questionsUpdated(counters[11])
                        .representativesAdded(counters[12]).representativesRemoved(counters[13]).representativesUpdated(counters[14])
                        .responsesCreated(responsesCreated)
                        .responsesUpdated(responsesUpdated)
                        .responsesDeleted(responsesDeleted)
                        .notificationsSent(notificationsSent)
                        .warnings(warnings)
                        .blockedReasons(blockedReasons)
                        .build())
                .build();
    }
}


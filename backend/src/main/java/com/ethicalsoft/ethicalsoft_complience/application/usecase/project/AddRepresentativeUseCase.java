package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RoleRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentStagePolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.RoleMappingPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.UserResolutionPolicy;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddRepresentativeUseCase {

    private final RepresentativeRepository representativeRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserPort currentUserPort;
    private final UserResolutionPolicy userResolutionPolicy;
    private final RoleMappingPolicy roleMappingPolicy;
    private final ProjectCurrentStagePolicy projectCurrentStagePolicy;
    private final SendNotificationUseCase sendNotificationUseCase;

    @Transactional
    public Set<Representative> execute(Project project, Set<RepresentativeDTO> repDTOs) {
        return doExecute(project, repDTOs, false);
    }

    @Transactional
    public Set<Representative> executeDraft(Project project, Set<RepresentativeDTO> repDTOs) {
        return doExecute(project, repDTOs, true);
    }

    private Set<Representative> doExecute(Project project, Set<RepresentativeDTO> repDTOs, boolean draft) {
        try {
            Project targetProject = java.util.Objects.requireNonNull(project);
            Set<RepresentativeDTO> safeRepDTOs = Optional.ofNullable(repDTOs).orElse(Set.of());
            log.info("[usecase-add-representative] Adicionando representantes para projeto id={} quantidade={} draft={}",
                targetProject.getId(), safeRepDTOs.size(), draft);

            if (ObjectUtils.isNullOrEmpty(safeRepDTOs)) {
                return new HashSet<>();
            }

            Map<Long, Role> resolvedRoles = resolveRoles(safeRepDTOs);
            User currentAdmin = currentUserPort.getCurrentUser();

            Set<Representative> representatives = safeRepDTOs.stream()
                .map(dto -> processRepresentative(dto, targetProject, resolvedRoles, currentAdmin, draft))
                    .collect(Collectors.toSet());

            log.info("[usecase-add-representative] {} representantes vinculados ao projeto id={}", representatives.size(), targetProject.getId());
            return representatives;
        } catch (Exception ex) {
            log.error("[usecase-add-representative] Falha ao criar representantes para projeto id={}",
                    project != null ? project.getId() : null, ex);
            throw ex;
        }
    }

    private Map<Long, Role> resolveRoles(Set<RepresentativeDTO> repDTOs) {
        Set<Long> requestedRoleIds = repDTOs.stream()
                .filter(dto -> dto.getRoleIds() != null)
                .flatMap(dto -> dto.getRoleIds().stream())
                .collect(Collectors.toSet());

        if (requestedRoleIds.isEmpty()) return Map.of();

        Map<Long, Role> roles = roleRepository.findAllById(requestedRoleIds).stream()
            .collect(Collectors.toMap(role -> role.getId(), Function.identity()));

        if (roles.size() != requestedRoleIds.size()) {
            throw new EntityNotFoundException("Um ou mais papéis (Roles) não foram encontrados.");
        }
        return roles;
    }

    private Representative processRepresentative(RepresentativeDTO dto, Project project, Map<Long, Role> resolvedRoles, User currentAdmin, boolean draft) {
        UserResolutionPolicy.UserResolutionResult resolution = userResolutionPolicy.resolveOrCreateUser(dto);
        Set<Role> roles = roleMappingPolicy.mapRoles(dto.getRoleIds(), resolvedRoles);

        Representative rep = new Representative();
        rep.setProject(project);
        rep.setUser(resolution.user());
        rep.setRoles(roles);
        rep.setWeight(dto.getWeight());
        rep.setCreationDate(LocalDate.now());

        representativeRepository.save(rep);

        notifyRepresentative(resolution, rep, project, currentAdmin, draft);

        return rep;
    }

    public void notifyProjectAssignmentsAfterPublish(Project project, Set<Representative> representatives) {
        if (project == null || representatives == null || representatives.isEmpty()) {
            return;
        }

        User currentAdmin = currentUserPort.getCurrentUser();
        runAfterCommit(() -> representatives.forEach(rep -> sendProjectAssignmentNotification(rep, project, currentAdmin)));
    }

    private void notifyRepresentative(UserResolutionPolicy.UserResolutionResult resolution,
                                      Representative rep,
                                      Project project,
                                      User currentAdmin,
                                      boolean draft) {
        Runnable sendNotifications = () -> {
            sendNewUserCredentialsNotification(resolution, rep, currentAdmin);
            if (!draft) {
                sendProjectAssignmentNotification(rep, project, currentAdmin);
            }
        };

        runAfterCommit(sendNotifications);
    }

    private void sendNewUserCredentialsNotification(UserResolutionPolicy.UserResolutionResult resolution,
                                                    Representative rep,
                                                    User currentAdmin) {
        resolution.temporaryPassword().ifPresent(tempPassword -> {
            Map<String, Object> ctx = new java.util.HashMap<>();
            ctx.put("to", rep.getUser().getEmail());
            ctx.put("firstName", Optional.ofNullable(rep.getUser().getFirstName()).orElse(""));
            ctx.put("tempPassword", tempPassword);
            ctx.put("projectName", rep.getProject() != null ? rep.getProject().getName() : "");
            ctx.put("adminName", buildAdminName(currentAdmin));
            ctx.put("projectId", rep.getProject() != null ? rep.getProject().getId() : null);
            ctx.put("systemTriggered", true);

            sendNotificationUseCase.execute(new SendNotificationCommand(
                    NotificationType.NEW_USER_CREDENTIALS,
                    ctx
            ));
        });
    }

    private void sendProjectAssignmentNotification(Representative rep, Project project, User currentAdmin) {
        Map<String, Object> ctx = new java.util.HashMap<>();
        ctx.put("to", rep.getUser().getEmail());
        ctx.put("firstName", Optional.ofNullable(rep.getUser().getFirstName()).orElse(""));
        ctx.put("projectName", Optional.ofNullable(project.getName()).orElse(""));
        ctx.put("projectId", project.getId());
        ctx.put("adminName", buildAdminName(currentAdmin));
        ctx.put("adminEmail", currentAdmin != null ? currentAdmin.getEmail() : "");
        ctx.put("roles", Optional.ofNullable(rep.getRoles()).orElse(Set.of()).stream().map(role -> role.getName()).toList());
        ctx.put("timelineSummary", Optional.ofNullable(project.getCurrentSituation()).orElse(""));
        ctx.put("startDate", project.getStartDate());
        ctx.put("deadline", project.getDeadline());
        ctx.put("nextQuestionnaireDate", projectCurrentStagePolicy.findNextQuestionnaireDate(project));
        ctx.put("systemTriggered", true);

        sendNotificationUseCase.execute(new SendNotificationCommand(
                NotificationType.PROJECT_ASSIGNMENT,
                ctx
        ));
    }

    private void runAfterCommit(Runnable action) {
        Runnable safeAction = () -> {
            try {
                action.run();
            } catch (Exception ex) {
                log.warn("[usecase-add-representative] Falha ao enviar notificações pós-commit", ex);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeAction.run();
                }
            });
        } else {
            safeAction.run();
        }
    }

    private String buildAdminName(User currentAdmin) {
        if (currentAdmin == null) {
            return "Sistema";
        }
        return Optional.ofNullable(currentAdmin.getFirstName()).orElse("") + " "
                + Optional.ofNullable(currentAdmin.getLastName()).orElse("");
    }
}
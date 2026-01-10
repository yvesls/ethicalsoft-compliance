package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.NotificationDispatcherPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.dto.NewUserCredentialsNotificationDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.dto.ProjectAssignmentNotificationDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectCurrentStagePolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.RoleMappingPolicy;
import com.ethicalsoft.ethicalsoft_complience.domain.service.UserResolutionPolicy;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RoleRepository;
import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
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
    private final NotificationDispatcherPort notificationDispatcher;
    private final com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.RepresentativeQuestionnaireResponseCommandPort representativeQuestionnaireResponseCommandPort;

    @Transactional
    public Set<Representative> execute(Project project, Set<RepresentativeDTO> repDTOs) {
        try {
            log.info("[usecase-add-representative] Adicionando representantes para projeto id={} quantidade={}",
                    project != null ? project.getId() : null, repDTOs != null ? repDTOs.size() : 0);

            if (ObjectUtils.isNullOrEmpty(repDTOs)) {
                return new HashSet<>();
            }
            if (ObjectUtils.isNullOrEmpty(project)) {
                throw new BusinessException("Projeto inválido para criação de representantes.");
            }

            Map<Long, Role> resolvedRoles = resolveRoles(repDTOs);
            User currentAdmin = currentUserPort.getCurrentUser();

            Set<Representative> representatives = repDTOs.stream()
                    .map(dto -> processRepresentative(dto, project, resolvedRoles, currentAdmin))
                    .collect(Collectors.toSet());

            log.info("[usecase-add-representative] {} representantes vinculados ao projeto id={}", representatives.size(), project.getId());
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
                .collect(Collectors.toMap(Role::getId, Function.identity()));

        if (roles.size() != requestedRoleIds.size()) {
            throw new EntityNotFoundException("Um ou mais papéis (Roles) não foram encontrados.");
        }
        return roles;
    }

    private Representative processRepresentative(RepresentativeDTO dto, Project project, Map<Long, Role> resolvedRoles, User currentAdmin) {
        UserResolutionPolicy.UserResolutionResult resolution = userResolutionPolicy.resolveOrCreateUser(dto);
        Set<Role> roles = roleMappingPolicy.mapRoles(dto.getRoleIds(), resolvedRoles);

        Representative rep = new Representative();
        rep.setProject(project);
        rep.setUser(resolution.user());
        rep.setRoles(roles);
        rep.setWeight(dto.getWeight());
        rep.setCreationDate(LocalDate.now());

        representativeRepository.save(rep);
        representativeQuestionnaireResponseCommandPort.createResponsesForRepresentative(project, rep);

        notifyUser(resolution, rep, project, currentAdmin);

        return rep;
    }

    private void notifyUser(UserResolutionPolicy.UserResolutionResult resolution, Representative rep, Project project, User currentAdmin) {
        resolution.temporaryPassword().ifPresent(tempPassword -> {
            NewUserCredentialsNotificationDTO credentialsDTO = new NewUserCredentialsNotificationDTO(
                    rep.getUser().getEmail(),
                    rep.getUser().getFirstName(),
                    tempPassword,
                    rep.getProject().getName(),
                    currentAdmin.getFirstName() + " " + currentAdmin.getLastName()
            );
            notificationDispatcher.dispatchNewUserCredentials(credentialsDTO);
        });

        ProjectAssignmentNotificationDTO assignmentDTO = new ProjectAssignmentNotificationDTO(
                rep.getUser().getEmail(),
                rep.getUser().getFirstName(),
                project.getName(),
                project.getId(),
                currentAdmin.getFirstName() + " " + currentAdmin.getLastName(),
                currentAdmin.getEmail(),
                rep.getRoles().stream().map(Role::getName).toList(),
                project.getCurrentSituation(),
                project.getStartDate(),
                project.getDeadline(),
                projectCurrentStagePolicy.findNextQuestionnaireDate(project)
        );
        notificationDispatcher.dispatchProjectAssignment(assignmentDTO);
    }
}
package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationRoleResolver {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public List<String> resolveRoles(String email, Long projectId) {
        if (email == null) return List.of();
        return userRepository.findByEmail(email)
                .map(user -> {
                    List<String> roles = new ArrayList<>();
                    if (user.getRole() == UserRoleEnum.ADMIN) {
                        roles.add(UserRoleEnum.ADMIN.name());
                    } else {
                        roles.add(UserRoleEnum.USER.name());
                    }
                    if (projectId != null) {
                        roles.addAll(findRepresentativeRoles(projectId, user.getId()));
                    }
                    return roles;
                })
                .orElse(List.of());
    }

    private List<String> findRepresentativeRoles(Long projectId, Long userId) {
        if (projectId == null || userId == null) return List.of();
        return projectRepository.findById(projectId)
                .map(Project::getRepresentatives)
                .orElse(Set.of())
                .stream()
                .filter(rep -> rep.getUser() != null && Objects.equals(rep.getUser().getId(), userId))
                .map(Representative::getRoles)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .map(r -> r.getName())
                .collect(Collectors.toList());
    }
}


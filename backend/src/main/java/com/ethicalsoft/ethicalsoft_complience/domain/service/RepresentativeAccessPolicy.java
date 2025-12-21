package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.domain.repository.ProjectRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.RepresentativeRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RepresentativeAccessPolicy {

    private final ProjectRepositoryPort projectRepository;
    private final RepresentativeRepositoryPort representativeRepository;
    private final AuthService authService;

    public Long resolveRepresentativeId(Long projectId) {
        User authenticated = authService.getAuthenticatedUser();
        if (UserRoleEnum.ADMIN.equals(authenticated.getRole()) || projectRepository.existsByIdAndOwnerId(projectId, authenticated.getId())) {
            return null;
        }
        Representative representative = representativeRepository.findByUserIdAndProjectId(authenticated.getId(), projectId)
                .orElseThrow(() -> new BusinessException("Usuário autenticado não possui representante vinculado ao projeto"));
        return representative.getId();
    }

    public void ensureRepresentativeBelongsToProject(Long representativeId, Project project) {
        Representative representative = representativeRepository.findById(representativeId)
                .orElseThrow(() -> new BusinessException("Representante não encontrado"));
        if (!Objects.equals(representative.getProject().getId(), project.getId())) {
            throw new BusinessException("Representante não pertence ao projeto informado");
        }
    }
}


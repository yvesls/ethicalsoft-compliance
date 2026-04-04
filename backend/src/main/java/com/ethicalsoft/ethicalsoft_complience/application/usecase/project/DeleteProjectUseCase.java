package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteProjectUseCase {

    private final ProjectRepository projectRepository;
    private final CurrentUserPort currentUserPort;

    @Transactional
    public void execute(Long projectId) {
        User currentUser = currentUserPort.getCurrentUser();
        log.info("[delete-project] Usuário id={} solicitou exclusão lógica do projeto id={}", currentUser.getId(), projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

        validatePermission(currentUser, project);
        validateDeletable(project);

        project.setStatus(ProjectStatusEnum.EXCLUIDO);
        project.setClosingDate(LocalDate.now());
        projectRepository.save(project);

        log.info("[delete-project] Projeto id={} marcado como EXCLUIDO com sucesso por userId={}", projectId, currentUser.getId());
    }

    private void validatePermission(User currentUser, Project project) {
        boolean isAdmin = UserRoleEnum.ADMIN.equals(currentUser.getRole());
        boolean isOwner = project.getOwner() != null && currentUser.getId().equals(project.getOwner().getId());

        if (!isAdmin && !isOwner) {
            throw new BusinessException("Apenas o administrador ou proprietário do projeto pode excluí-lo.");
        }
    }

    private void validateDeletable(Project project) {
        if (ProjectStatusEnum.EXCLUIDO.equals(project.getStatus())) {
            throw new BusinessException("O projeto já está excluído.");
        }

        if (ProjectStatusEnum.CONCLUIDO.equals(project.getStatus())) {
            throw new BusinessException("Projetos concluídos não podem ser excluídos. Utilize a função de arquivar se necessário.");
        }
    }
}


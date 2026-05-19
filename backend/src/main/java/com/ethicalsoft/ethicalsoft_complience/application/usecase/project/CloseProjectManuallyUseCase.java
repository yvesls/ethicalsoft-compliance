package com.ethicalsoft.ethicalsoft_complience.application.usecase.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.ProjectIsepResult;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.CloseProjectResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.ProjectIsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepMath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloseProjectManuallyUseCase {

    private final ProcessExpiredProjectIsepUseCase processExpiredProjectIsepUseCase;
    private final ProjectIsepResultQueryPort projectIsepResultQueryPort;

    @Transactional
    public CloseProjectResponseDTO execute(Long projectId) {
        if (projectIsepResultQueryPort.existsByProjectId(projectId)) {
            throw new IllegalStateException("O projeto id=" + projectId + " já possui ISEP consolidado calculado.");
        }

        String closedBy = resolveClosedBy();
        log.info("[close-project] Encerramento manual do projeto id={} por '{}'", projectId, closedBy);

        ProjectIsepResult result = processExpiredProjectIsepUseCase.forceCloseProject(projectId, closedBy);
        return new CloseProjectResponseDTO(
                projectId,
                IsepMath.toPercent(result.getIsep()).toPlainString(),
                result.getBand(),
                result.getQuestionnaireCount(),
                result.getCalculatedAt(),
                result.getClosedBy() != null ? result.getClosedBy() : ""
        );
    }

    private String resolveClosedBy() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                return auth.getName();
            }
        } catch (Exception ignored) {
            log.warn("[close-project] Falha ao resolver usuário autenticado para encerramento manual do projeto. Usando valor padrão.");
        }
        return "Administrador (manual)";
    }
}

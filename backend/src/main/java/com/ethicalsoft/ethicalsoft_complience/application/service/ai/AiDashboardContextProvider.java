package com.ethicalsoft.ethicalsoft_complience.application.service.ai;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.DashboardSnapshotAssembler;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model.DashboardSnapshot;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.GetJustificationWordCloudUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.GetQuestionnaireDashboardUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.GetRoleStageComplianceUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.QuestionnaireIsepDashboardDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.RoleStageComplianceDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.WordCloudDTO;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiDashboardContextProvider {

    private final ProjectRepository projectRepository;
    private final GetQuestionnaireDashboardUseCase questionnaireDashboardUseCase;
    private final GetRoleStageComplianceUseCase roleStageComplianceUseCase;
    private final GetJustificationWordCloudUseCase wordCloudUseCase;
    private final DashboardSnapshotAssembler snapshotAssembler;

    public DashboardSnapshot buildSnapshot(Long projectId, Integer questionnaireId) {
        log.debug("[ai-context] Construindo snapshot para projeto={} questionário={}",
                projectId, questionnaireId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + projectId));

        QuestionnaireIsepDashboardDTO dashboard =
                questionnaireDashboardUseCase.execute(projectId, questionnaireId);

        List<RoleStageComplianceDTO> roleStage =
                roleStageComplianceUseCase.execute(projectId, questionnaireId);

        WordCloudDTO wordCloud =
                wordCloudUseCase.execute(projectId, questionnaireId);

        return snapshotAssembler.assemble(
                projectId,
                project.getName(),
                project.getType() != null ? project.getType().name() : null,
                dashboard,
                roleStage,
                wordCloud
        );
    }
}


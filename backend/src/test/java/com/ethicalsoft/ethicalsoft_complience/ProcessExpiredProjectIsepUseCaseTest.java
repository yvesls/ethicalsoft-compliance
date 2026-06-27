package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.ProjectIsepResultCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.ProjectIsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.project.ProcessExpiredProjectIsepUseCase;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessExpiredProjectIsepUseCaseTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private IsepResultQueryPort isepResultQueryPort;
    @Mock
    private ProjectIsepResultCommandPort projectIsepResultCommandPort;
    @Mock
    private ProjectIsepResultQueryPort projectIsepResultQueryPort;
    @Mock
    private SendNotificationUseCase sendNotificationUseCase;

    @InjectMocks
    private ProcessExpiredProjectIsepUseCase useCase;

    private Project project;
    private Questionnaire q1;
    private Questionnaire q2;
    private QuestionnaireResult result1;
    private QuestionnaireResult result2;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setFirstName("Dono");
        owner.setLastName("Projeto");

        q1 = new Questionnaire();
        q1.setId(10);
        q1.setName("Sprint 1");
        q1.setWeight(1);

        q2 = new Questionnaire();
        q2.setId(11);
        q2.setName("Sprint 2");
        q2.setWeight(2);

        project = new Project();
        project.setId(100L);
        project.setName("Projeto BR04");
        project.setType(ProjectTypeEnum.ITERATIVO);
        project.setOwner(owner);
        project.setDeadline(LocalDate.now().minusDays(2));
        project.setStatus(ProjectStatusEnum.ABERTO);
        project.setQuestionnaires(Set.of(q1, q2));

        result1 = new QuestionnaireResult();
        result1.setId(1L);
        result1.setProjectId(100L);
        result1.setQuestionnaireId(10);
        result1.setIseq(BigDecimal.valueOf(0.90));
        result1.setBand(EthicalComplianceBand.A.name());
        result1.setCalculatedAt(LocalDateTime.now().minusDays(1));

        result2 = new QuestionnaireResult();
        result2.setId(2L);
        result2.setProjectId(100L);
        result2.setQuestionnaireId(11);
        result2.setIseq(BigDecimal.valueOf(0.75));
        result2.setBand(EthicalComplianceBand.B.name());
        result2.setCalculatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void execute_noExpiredProjects_doesNothing() {
        when(projectRepository.findExpiredWithoutIsepResult(any(), any(), any()))
                .thenReturn(List.of());

        useCase.execute();

        verifyNoInteractions(isepResultQueryPort, projectIsepResultCommandPort, sendNotificationUseCase);
    }

    @Test
    void execute_allQuestionnairesCompleted_finalizesProjectAndNotifies() {
        when(projectRepository.findExpiredWithoutIsepResult(any(), any(), any()))
                .thenReturn(List.of(project));

        when(isepResultQueryPort.findByProjectId(100L))
                .thenReturn(List.of(result1, result2));

        ProjectIsepResult saved = new ProjectIsepResult();
        saved.setId(1L);
        saved.setProjectId(100L);
        saved.setIsep(BigDecimal.valueOf(0.80));
        saved.setBand(EthicalComplianceBand.B.name());
        saved.setQuestionnaireCount(2);
        saved.setCalculatedAt(LocalDateTime.now());
        saved.setClosedBy("Sistema (Scheduler)");
        when(projectIsepResultCommandPort.save(any())).thenReturn(saved);
        when(projectRepository.save(any())).thenReturn(project);
        doNothing().when(sendNotificationUseCase).execute(any());

        useCase.execute();

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getStatus()).isEqualTo(ProjectStatusEnum.CONCLUIDO);
        assertThat(projectCaptor.getValue().getTimelineStatus()).isEqualTo(TimelineStatusEnum.CONCLUIDO);

        ArgumentCaptor<ProjectIsepResult> resultCaptor = ArgumentCaptor.forClass(ProjectIsepResult.class);
        verify(projectIsepResultCommandPort).save(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getQuestionnaireCount()).isEqualTo(2);
        assertThat(resultCaptor.getValue().getIsep()).isNotNull();

        verify(sendNotificationUseCase, atLeastOnce()).execute(any());
    }

    @Test
    void execute_notAllQuestionnairesCompleted_marksProjectAsDelayed() {
        when(projectRepository.findExpiredWithoutIsepResult(any(), any(), any()))
                .thenReturn(List.of(project));

        when(isepResultQueryPort.findByProjectId(100L))
                .thenReturn(List.of(result1));

        when(projectRepository.save(any())).thenReturn(project);
        doNothing().when(sendNotificationUseCase).execute(any());

        useCase.execute();

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getTimelineStatus()).isEqualTo(TimelineStatusEnum.ATRASADO);

        verifyNoInteractions(projectIsepResultCommandPort);

        verify(sendNotificationUseCase, atLeastOnce()).execute(any());
    }

    @Test
    void forceCloseProject_withAtLeastOneResult_calculatesAndClosesProject() {
        when(projectRepository.findByIdWithRepresentativesAndQuestionnaires(100L))
                .thenReturn(Optional.of(project));

        when(isepResultQueryPort.findByProjectId(100L))
                .thenReturn(List.of(result1));
        ProjectIsepResult saved = new ProjectIsepResult();
        saved.setId(1L);
        saved.setProjectId(100L);
        saved.setIsep(BigDecimal.valueOf(0.90));
        saved.setBand(EthicalComplianceBand.A.name());
        saved.setQuestionnaireCount(1);
        saved.setCalculatedAt(LocalDateTime.now());
        saved.setClosedBy("admin@example.com");
        when(projectIsepResultCommandPort.save(any())).thenReturn(saved);
        when(projectRepository.save(any())).thenReturn(project);
        doNothing().when(sendNotificationUseCase).execute(any());

        ProjectIsepResult result = useCase.forceCloseProject(100L, "admin@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getBand()).isEqualTo(EthicalComplianceBand.A.name());
        verify(projectIsepResultCommandPort).save(any());
        verify(projectRepository).save(any());
    }

    @Test
    void forceCloseProject_noQuestionnaireResults_throwsIllegalState() {
        when(projectRepository.findByIdWithRepresentativesAndQuestionnaires(100L))
                .thenReturn(Optional.of(project));

        when(isepResultQueryPort.findByProjectId(100L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> useCase.forceCloseProject(100L, "admin@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nenhum questionário calculado");

        verifyNoInteractions(projectIsepResultCommandPort);
    }

    @Test
    void forceCloseProject_projectNotFound_throwsIllegalArgument() {
        when(projectRepository.findByIdWithRepresentativesAndQuestionnaires(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.forceCloseProject(999L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Projeto não encontrado");
    }

    @Test
    void tryFinalizeProjectAfterQuestionnaire_alreadyHasIsep_skips() {
        when(projectRepository.findByIdWithRepresentativesAndQuestionnaires(100L))
                .thenReturn(Optional.of(project));
        when(projectIsepResultQueryPort.existsByProjectId(100L)).thenReturn(true);

        useCase.tryFinalizeProjectAfterQuestionnaire(100L);

        verifyNoInteractions(isepResultQueryPort, projectIsepResultCommandPort);
    }

    @Test
    void tryFinalizeProjectAfterQuestionnaire_allCompleted_finalizesProject() {
        when(projectRepository.findByIdWithRepresentativesAndQuestionnaires(100L))
                .thenReturn(Optional.of(project));
        when(projectIsepResultQueryPort.existsByProjectId(100L)).thenReturn(false);
        when(isepResultQueryPort.findByProjectId(100L)).thenReturn(List.of(result1, result2));

        ProjectIsepResult saved = new ProjectIsepResult();
        saved.setId(1L);
        saved.setProjectId(100L);
        saved.setIsep(BigDecimal.valueOf(0.80));
        saved.setBand(EthicalComplianceBand.B.name());
        saved.setQuestionnaireCount(2);
        saved.setCalculatedAt(LocalDateTime.now());
        saved.setClosedBy("Sistema (auto-finalização)");
        when(projectIsepResultCommandPort.save(any())).thenReturn(saved);
        when(projectRepository.save(any())).thenReturn(project);
        doNothing().when(sendNotificationUseCase).execute(any());

        useCase.tryFinalizeProjectAfterQuestionnaire(100L);

        verify(projectIsepResultCommandPort).save(any());
        verify(sendNotificationUseCase, atLeastOnce()).execute(any());
    }

    @Test
    void execute_isepConsolidatedIsWeightedAverage() {
        when(projectRepository.findExpiredWithoutIsepResult(any(), any(), any()))
                .thenReturn(List.of(project));
        when(isepResultQueryPort.findByProjectId(100L))
                .thenReturn(List.of(result1, result2));

        ArgumentCaptor<ProjectIsepResult> captor = ArgumentCaptor.forClass(ProjectIsepResult.class);
        when(projectIsepResultCommandPort.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any())).thenReturn(project);
        doNothing().when(sendNotificationUseCase).execute(any());

        useCase.execute();

        ProjectIsepResult saved = captor.getValue();
        assertThat(saved.getBand()).isEqualTo(EthicalComplianceBand.B.name());
        assertThat(saved.getIsep().stripTrailingZeros())
                .isEqualByComparingTo(BigDecimal.valueOf(0.80).stripTrailingZeros());
    }
}


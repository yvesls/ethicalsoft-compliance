package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.SendNotificationUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.project.ProcessExpiredProjectIsepUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.ProcessExpiredQuestionnairesIsepUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.ProcessQuestionnaireIsepUseCase;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessExpiredQuestionnairesIsepUseCaseTest {

    @Mock
    private QuestionnaireRepository questionnaireRepository;
    @Mock
    private QuestionnaireResponseRepository questionnaireResponseRepository;
    @Mock
    private ProcessQuestionnaireIsepUseCase processQuestionnaireIsepUseCase;
    @Mock
    private IsepResultQueryPort isepResultQueryPort;
    @Mock
    private SendNotificationUseCase sendNotificationUseCase;
    @Mock
    private ProcessExpiredProjectIsepUseCase processExpiredProjectIsepUseCase;

    @InjectMocks
    private ProcessExpiredQuestionnairesIsepUseCase useCase;

    private Questionnaire questionnaire;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setFirstName("Admin");
        user.setLastName("User");

        Project project = new Project();
        project.setId(10L);
        project.setName("Projeto Teste");
        project.setType(ProjectTypeEnum.ITERATIVO);
        project.setOwner(user);

        Representative representative = new Representative();
        representative.setId(100L);
        representative.setUser(user);
        representative.setProject(project);
        project.setRepresentatives(Set.of(representative));

        questionnaire = new Questionnaire();
        questionnaire.setId(20);
        questionnaire.setName("Sprint 1");
        questionnaire.setProject(project);
        questionnaire.setApplicationEndDate(LocalDate.now().minusDays(1));
    }

    @Test
    void execute_noExpiredQuestionnaires_skips() {
        when(questionnaireRepository.findExpiredWithoutIsepResult(any())).thenReturn(List.of());

        useCase.execute();

        verifyNoInteractions(processQuestionnaireIsepUseCase);
        verifyNoInteractions(sendNotificationUseCase);
    }

    @Test
    void execute_questionnaireWithoutProject_skips() {
        questionnaire.setProject(null);
        when(questionnaireRepository.findExpiredWithoutIsepResult(any())).thenReturn(List.of(questionnaire));

        useCase.execute();

        verifyNoInteractions(processQuestionnaireIsepUseCase);
    }

    @Test
    void execute_isepCalculated_notifiesSuccess() {
        when(questionnaireRepository.findExpiredWithoutIsepResult(any())).thenReturn(List.of(questionnaire));
        when(processQuestionnaireIsepUseCase.processIfComplete(10L, 20)).thenReturn(true);

        QuestionnaireResult mockResult = new QuestionnaireResult();
        mockResult.setQuestionnaireId(20);
        mockResult.setIseq(BigDecimal.valueOf(0.85));
        mockResult.setBand(EthicalComplianceBand.B.name());
        mockResult.setCalculatedAt(LocalDateTime.now());
        when(isepResultQueryPort.findByQuestionnaireId(20)).thenReturn(Optional.of(mockResult));
        doNothing().when(sendNotificationUseCase).execute(any());
        doNothing().when(processExpiredProjectIsepUseCase).tryFinalizeProjectAfterQuestionnaire(10L);

        useCase.execute();

        verify(processQuestionnaireIsepUseCase).processIfComplete(10L, 20);
        verify(sendNotificationUseCase, atLeastOnce()).execute(any());
        verify(processExpiredProjectIsepUseCase).tryFinalizeProjectAfterQuestionnaire(10L);
    }

    @Test
    void execute_isepNotCalculated_marksDelayedAndNotifies() {
        when(questionnaireRepository.findExpiredWithoutIsepResult(any())).thenReturn(List.of(questionnaire));
        when(processQuestionnaireIsepUseCase.processIfComplete(10L, 20)).thenReturn(false);
        when(questionnaireRepository.save(any())).thenReturn(questionnaire);
        lenient().when(questionnaireRepository.findByProjectId(10L)).thenReturn(List.of(questionnaire));

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setRepresentativeId(100L);
        response.setStatus(QuestionnaireResponseStatus.COMPLETED);
        lenient().when(questionnaireResponseRepository.findByProjectIdAndQuestionnaireIdExcludingTemplates(10L, 20))
                .thenReturn(List.of(response));

        useCase.execute();

        verify(questionnaireRepository).save(questionnaire);
        verify(sendNotificationUseCase, atLeastOnce()).execute(any());
    }
}

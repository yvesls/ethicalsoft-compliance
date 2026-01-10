package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.IterationDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.StageDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.IterationCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.StageCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.ProjectQuestionnaireCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.service.strategy.project_strategy.IterativeProjectStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
class IterativeProjectStrategyTest {

    @Mock
    private StageCommandPort stageCommandPort;
    @Mock
    private IterationCommandPort iterationCommandPort;
    @Mock
    private ProjectQuestionnaireCommandPort projectQuestionnaireCommandPort;

    @InjectMocks
    private IterativeProjectStrategy strategy;

    @Captor
    private ArgumentCaptor<List<Stage>> stagesCaptor;
    @Captor
    private ArgumentCaptor<List<Iteration>> iterationsCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Stage>> stageMapCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Iteration>> iterationMapCaptor;

    private Project project;
    private ProjectCreationRequestDTO request;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId( 1L );
        project.setStages( new HashSet<>() );
        project.setIterations( new HashSet<>() );

        StageDTO stageDTO = new StageDTO();
        stageDTO.setName( "CategoryStage" );

        IterationDTO iterationDTO = new IterationDTO();
        iterationDTO.setName( "Sprint 1" );

        request = new ProjectCreationRequestDTO();
        request.setStages( List.of( stageDTO ) );
        request.setIterations( Set.of( iterationDTO ) );
        request.setQuestionnaires( Set.of() );
    }

    @Test
    void createStructure_shouldSaveStagesIterationsAndCallQuestionnairePort() {
        Stage savedStage = new Stage();
        savedStage.setId( 1 );
        savedStage.setName( "CategoryStage" );
        when( stageCommandPort.saveAll( any( List.class ) ) ).thenReturn( List.of( savedStage ) );

        Iteration savedIter = new Iteration();
        savedIter.setId( 1 );
        savedIter.setName( "Sprint 1" );
        when( iterationCommandPort.saveAll( any( List.class ) ) ).thenReturn( List.of( savedIter ) );

        strategy.createStructure( project, request );

        verify( stageCommandPort ).saveAll( stagesCaptor.capture() );
        assertEquals( 1, stagesCaptor.getValue().size() );
        assertEquals( project, stagesCaptor.getValue().get( 0 ).getProject() );

        verify( iterationCommandPort ).saveAll( iterationsCaptor.capture() );
        assertEquals( 1, iterationsCaptor.getValue().size() );
        assertEquals( project, iterationsCaptor.getValue().get( 0 ).getProject() );

        verify( projectQuestionnaireCommandPort ).createQuestionnaires( eq( project ), eq( request.getQuestionnaires() ), stageMapCaptor.capture(), iterationMapCaptor.capture() );

        Map<String, Stage> capturedStageMap = stageMapCaptor.getValue();
        assertEquals( 1, capturedStageMap.size() );
        assertTrue( capturedStageMap.containsKey( "CategoryStage" ) );
        assertEquals( savedStage, capturedStageMap.get( "CategoryStage" ) );

        Map<String, Iteration> capturedIterMap = iterationMapCaptor.getValue();
        assertEquals( 1, capturedIterMap.size() );
        assertTrue( capturedIterMap.containsKey( "Sprint 1" ) );
        assertEquals( savedIter, capturedIterMap.get( "Sprint 1" ) );
    }
}
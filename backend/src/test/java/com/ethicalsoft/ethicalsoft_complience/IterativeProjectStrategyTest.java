package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.IterationDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.StageDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.IterationRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.service.QuestionnaireService;
import com.ethicalsoft.ethicalsoft_complience.service.strategy.project_strategy.IterativeProjectStrategy;
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
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
class IterativeProjectStrategyTest {

	@Mock
	private StageRepository stageRepository;
	@Mock
	private IterationRepository iterationRepository;
	@Mock
	private QuestionnaireService questionnaireService;

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
	void createStructure_shouldSaveStagesIterationsAndCallQuestionnaireService() {
		Stage savedStage = new Stage();
		savedStage.setId( 1 );
		savedStage.setName( "CategoryStage" );
		when( stageRepository.saveAll( any( List.class ) ) ).thenReturn( List.of( savedStage ) );

		Iteration savedIter = new Iteration();
		savedIter.setId( 1 );
		savedIter.setName( "Sprint 1" );
		when( iterationRepository.saveAll( any( List.class ) ) ).thenReturn( List.of( savedIter ) );

		strategy.createStructure( project, request );

		verify( stageRepository ).saveAll( stagesCaptor.capture() );
		assertEquals( 1, stagesCaptor.getValue().size() );
		assertEquals( project, stagesCaptor.getValue().get( 0 ).getProject() );

		verify( iterationRepository ).saveAll( iterationsCaptor.capture() );
		assertEquals( 1, iterationsCaptor.getValue().size() );
		assertEquals( project, iterationsCaptor.getValue().get( 0 ).getProject() );

		verify( questionnaireService ).createQuestionnaires( eq( project ), eq( request.getQuestionnaires() ), stageMapCaptor.capture(), iterationMapCaptor.capture() );

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
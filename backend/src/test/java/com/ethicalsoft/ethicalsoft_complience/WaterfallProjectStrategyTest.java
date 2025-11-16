package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.StageDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.service.QuestionnaireService;
import com.ethicalsoft.ethicalsoft_complience.service.strategy.project_strategy.WaterfallProjectStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class WaterfallProjectStrategyTest {

	@Mock
	private StageRepository stageRepository;
	@Mock
	private QuestionnaireService questionnaireService;

	@InjectMocks
	private WaterfallProjectStrategy strategy;

	@Captor
	private ArgumentCaptor<List<Stage>> stagesCaptor;
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

		StageDTO stageDTO = new StageDTO();
		stageDTO.setName( "Requirements" );
		stageDTO.setWeight( BigDecimal.TEN );

		request = new ProjectCreationRequestDTO();
		request.setStages( List.of( stageDTO ) );
		request.setQuestionnaires( Set.of( new QuestionnaireDTO() ) );
	}

	@Test
	void createStructure_shouldSaveStagesAndCallQuestionnaireService() {
		Stage savedStage = new Stage();
		savedStage.setId( 1 );
		savedStage.setName( "Requirements" );
		savedStage.setProject( project );

		when( stageRepository.saveAll( any( List.class ) ) ).thenReturn( List.of( savedStage ) );

		strategy.createStructure( project, request );

		verify( stageRepository ).saveAll( stagesCaptor.capture() );
		List<Stage> savedStages = stagesCaptor.getValue();
		assertEquals( 1, savedStages.size() );
		assertEquals( project, savedStages.get( 0 ).getProject() );

		verify( questionnaireService ).createQuestionnaires( eq( project ), eq( request.getQuestionnaires() ), stageMapCaptor.capture(), iterationMapCaptor.capture() );

		Map<String, Stage> capturedStageMap = stageMapCaptor.getValue();
		assertEquals( 1, capturedStageMap.size() );
		assertTrue( capturedStageMap.containsKey( "Requirements" ) );
		assertEquals( savedStage, capturedStageMap.get( "Requirements" ) );

		Map<String, Iteration> capturedIterMap = iterationMapCaptor.getValue();
		assertTrue( capturedIterMap.isEmpty() );
	}
}
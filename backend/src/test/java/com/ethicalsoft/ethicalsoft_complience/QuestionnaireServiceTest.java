package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RoleRepository;
import com.ethicalsoft.ethicalsoft_complience.service.QuestionnaireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
class QuestionnaireServiceTest {

	@Mock
	private QuestionnaireRepository questionnaireRepository;

	@Mock
	private QuestionRepository questionRepository;

	@Mock
	private RoleRepository roleRepository;

	@InjectMocks
	private QuestionnaireService questionnaireService;

	private Project project;
	private Stage stage;
	private Iteration iteration;
	private Role role;

	private Map<String, Stage> stageMap;
	private Map<String, Iteration> iterationMap;

	private QuestionnaireDTO questionnaireDTO;
	private QuestionDTO questionDTO;

	@BeforeEach
	void setUp() {
		project = new Project();
		project.setId( 1L );
		stage = new Stage();
		stage.setId( 1 );
		stage.setName( "Requirements" );
		iteration = new Iteration();
		iteration.setId( 1 );
		iteration.setName( "Sprint 1" );
		role = new Role();
		role.setId( 1L );
		role.setName( "Developer" );

		stageMap = Map.of( "Requirements", stage );
		iterationMap = Map.of( "Sprint 1", iteration );

		questionDTO = new QuestionDTO();
		questionDTO.setValue( "Is the code clean?" );
		questionDTO.setCategoryStageName( "Requirements" );
		questionDTO.setRoleIds( Set.of( role.getId() ) );

		questionnaireDTO = new QuestionnaireDTO();
		questionnaireDTO.setName( "Sprint 1 Questionnaire" );
		questionnaireDTO.setQuestions( Set.of( questionDTO ) );

	}

	@Test
	void createQuestionnaires_forIterativeProject_shouldLinkToIteration() {
		when( roleRepository.findAll() ).thenReturn( List.of( role ) );
		when( questionnaireRepository.save( any( Questionnaire.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		questionnaireDTO.setIterationName( "Sprint 1" );
		questionnaireDTO.setStageName( null );

		questionnaireService.createQuestionnaires( project, Set.of( questionnaireDTO ), stageMap, iterationMap );

		ArgumentCaptor<Questionnaire> qnCaptor = ArgumentCaptor.forClass( Questionnaire.class );
		verify( questionnaireRepository ).save( qnCaptor.capture() );

		Questionnaire savedQN = qnCaptor.getValue();
		assertEquals( project, savedQN.getProject() );
		assertEquals( "Sprint 1 Questionnaire", savedQN.getName() );
		assertEquals( iteration, savedQN.getIterationRef() );
		assertNull( savedQN.getStage() );

		ArgumentCaptor<List<Question>> qCaptor = ArgumentCaptor.forClass( List.class );
		verify( questionRepository ).saveAll( qCaptor.capture() );

		List<Question> savedQuestions = qCaptor.getValue();
		assertEquals( 1, savedQuestions.size() );

		Question savedQ = savedQuestions.get( 0 );
		assertEquals( "Is the code clean?", savedQ.getValue() );
		assertEquals( savedQN, savedQ.getQuestionnaire() );
		assertEquals( stage, savedQ.getStage() );
		assertTrue( savedQ.getRoles().contains( role ) );
	}

	@Test
	void createQuestionnaires_forWaterfallProject_shouldLinkToStage() {
		when( roleRepository.findAll() ).thenReturn( List.of( role ) );
		when( questionnaireRepository.save( any( Questionnaire.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		questionnaireDTO.setStageName( "Requirements" );
		questionnaireDTO.setIterationName( null );

		questionnaireService.createQuestionnaires( project, Set.of( questionnaireDTO ), stageMap, iterationMap );

		ArgumentCaptor<Questionnaire> qnCaptor = ArgumentCaptor.forClass( Questionnaire.class );
		verify( questionnaireRepository ).save( qnCaptor.capture() );

		Questionnaire savedQN = qnCaptor.getValue();
		assertEquals( project, savedQN.getProject() );
		assertEquals( stage, savedQN.getStage() );
		assertNull( savedQN.getIterationRef() );
	}

	@Test
	void createQuestionnaires_shouldNotFail_whenMapsAreEmptyOrLinksAreMissing() {
		when( roleRepository.findAll() ).thenReturn( Collections.emptyList() );
		when( questionnaireRepository.save( any( Questionnaire.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		questionDTO.setCategoryStageName( "NonExistentStage" );
		questionnaireDTO.setIterationName( "NonExistentIteration" );
		questionDTO.setRoleIds( Set.of( 999L ) );

		questionnaireService.createQuestionnaires( project, Set.of( questionnaireDTO ), Collections.emptyMap(), Collections.emptyMap() );

		ArgumentCaptor<Questionnaire> qnCaptor = ArgumentCaptor.forClass( Questionnaire.class );
		verify( questionnaireRepository ).save( qnCaptor.capture() );

		assertNull( qnCaptor.getValue().getIterationRef() );
		assertNull( qnCaptor.getValue().getStage() );

		ArgumentCaptor<List<Question>> qCaptor = ArgumentCaptor.forClass( List.class );
		verify( questionRepository ).saveAll( qCaptor.capture() );

		Question savedQ = qCaptor.getValue().get( 0 );
		assertNull( savedQ.getStage() );
		assertTrue( savedQ.getRoles().isEmpty() );
	}

	@Test
	void createQuestionnaires_shouldDoNothing_whenDTOSetIsNull() {
		questionnaireService.createQuestionnaires( project, null, stageMap, iterationMap );

		verify( questionnaireRepository, never() ).save( any() );
		verify( questionRepository, never() ).saveAll( any() );
		verify( roleRepository, never() ).findAll();
	}
}
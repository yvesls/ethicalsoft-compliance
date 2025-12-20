package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
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
import java.util.concurrent.atomic.AtomicInteger;

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
    private AtomicInteger questionIdSequence;

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

		questionIdSequence = new AtomicInteger(1);
		lenient().when( questionRepository.save( any( Question.class ) ) ).thenAnswer( invocation -> {
			Question q = invocation.getArgument(0);
			if ( q.getId() == null ) {
				q.setId( questionIdSequence.getAndIncrement() );
			}
			return q;
		} );

		project.setType( ProjectTypeEnum.CASCATA );

	}

	@Test
	void createQuestionnaires_forIterativeProject_shouldLinkToIteration() {
		project.setType( ProjectTypeEnum.ITERATIVO );
		when( roleRepository.findAll() ).thenReturn( List.of( role ) );
		when( questionnaireRepository.save( any( Questionnaire.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		questionnaireDTO.setIterationName( "Sprint 1" );
		questionnaireDTO.setStageName( null );

		questionDTO.setStageNames(List.of("Requirements"));

		questionnaireService.createQuestionnaires( project, Set.of( questionnaireDTO ), stageMap, iterationMap );

		ArgumentCaptor<Questionnaire> qnCaptor = ArgumentCaptor.forClass( Questionnaire.class );
		verify( questionnaireRepository ).save( qnCaptor.capture() );

		Questionnaire savedQN = qnCaptor.getValue();
		assertEquals( project, savedQN.getProject() );
		assertEquals( "Sprint 1 Questionnaire", savedQN.getName() );
		assertEquals( iteration, savedQN.getIterationRef() );
		assertNull( savedQN.getStage() );

		ArgumentCaptor<Question> qCaptor = ArgumentCaptor.forClass( Question.class );
		verify( questionRepository, times( 1 ) ).save( qCaptor.capture() );

		Question savedQ = qCaptor.getValue();
		assertEquals( "Is the code clean?", savedQ.getValue() );
		assertEquals( savedQN, savedQ.getQuestionnaire() );
		assertTrue( savedQ.getStages().contains( stage ) );
		assertTrue( savedQ.getRoles().contains( role ) );
	}

	@Test
	void createQuestionnaires_forWaterfallProject_shouldLinkToStage() {
		project.setType( ProjectTypeEnum.CASCATA );
		when( roleRepository.findAll() ).thenReturn( List.of( role ) );
		when( questionnaireRepository.save( any( Questionnaire.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		questionnaireDTO.setStageName( "Requirements" );
		questionnaireDTO.setIterationName( null );

		questionDTO.setStageNames(null);

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
		project.setType( ProjectTypeEnum.CASCATA );
		when( roleRepository.findAll() ).thenReturn( Collections.emptyList() );
		when( questionnaireRepository.save( any( Questionnaire.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );

		questionDTO.setCategoryStageName( "NonExistentStage" );
		questionnaireDTO.setIterationName( "NonExistentIteration" );
		questionDTO.setRoleIds( Set.of( 999L ) );
		questionDTO.setStageNames(List.of());

		questionnaireService.createQuestionnaires( project, Set.of( questionnaireDTO ), Collections.emptyMap(), Collections.emptyMap() );

		ArgumentCaptor<Questionnaire> qnCaptor = ArgumentCaptor.forClass( Questionnaire.class );
		verify( questionnaireRepository ).save( qnCaptor.capture() );

		assertNull( qnCaptor.getValue().getIterationRef() );
		assertNull( qnCaptor.getValue().getStage() );

		ArgumentCaptor<Question> qCaptor = ArgumentCaptor.forClass( Question.class );
		verify( questionRepository, times( 1 ) ).save( qCaptor.capture() );

		Question savedQ = qCaptor.getValue();
		assertTrue( savedQ.getStages() == null || savedQ.getStages().isEmpty() );
		assertTrue( savedQ.getRoles().isEmpty() );
	}

	@Test
	void createQuestionnaires_shouldDoNothing_whenDTOSetIsNull() {
		questionnaireService.createQuestionnaires( project, null, stageMap, iterationMap );

		verify( questionnaireRepository, never() ).save( any() );
		verify( questionRepository, never() ).save( any() );
		verify( roleRepository, never() ).findAll();
	}
}
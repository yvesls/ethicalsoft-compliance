package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponseDocument;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RoleRepository;
import com.ethicalsoft.ethicalsoft_complience.util.ModelMapperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionnaireService {

	private final QuestionnaireRepository questionnaireRepository;
	private final RoleRepository roleRepository;
	private final QuestionnaireResponseRepository questionnaireResponseRepository;

	@Transactional( propagation = Propagation.MANDATORY )
	public void createQuestionnaires( Project project, Set<QuestionnaireDTO> questionnaireDTOs, Map<String, Stage> stageMap, Map<String, Iteration> iterationMap ) {

		if ( questionnaireDTOs == null || questionnaireDTOs.isEmpty() ) {
			return;
		}

		Map<Long, Role> rolesById = roleRepository.findAll().stream()
				.collect( Collectors.toMap( Role::getId, role -> role ) );

		for ( QuestionnaireDTO qnDto : questionnaireDTOs ) {

			Questionnaire questionnaire = ModelMapperUtils.map( qnDto, Questionnaire.class );

			questionnaire.setProject( project );

			if ( qnDto.getStageName() != null ) {
				questionnaire.setStage( stageMap.get( qnDto.getStageName() ) );
			} else if ( qnDto.getIterationName() != null ) {
				questionnaire.setIterationRef( iterationMap.get( qnDto.getIterationName() ) );
			}

			Questionnaire savedQuestionnaire = questionnaireRepository.save( questionnaire );

			List<QuestionnaireResponseDocument.AnswerDocument> answerTemplate = buildAnswerTemplate( qnDto.getQuestions(), stageMap, rolesById );

			createInitialResponses( project, savedQuestionnaire, answerTemplate );
		}
	}

	private List<QuestionnaireResponseDocument.AnswerDocument> buildAnswerTemplate( Set<QuestionDTO> questionDTOs,
			Map<String, Stage> stageMap,
			Map<Long, Role> rolesById ) {
		if ( questionDTOs == null || questionDTOs.isEmpty() ) {
			return Collections.emptyList();
		}

		AtomicLong questionCounter = new AtomicLong( 1L );

		return questionDTOs.stream().map( dto -> {
			QuestionnaireResponseDocument.AnswerDocument answer = new QuestionnaireResponseDocument.AnswerDocument();
			answer.setQuestionId( questionCounter.getAndIncrement() );
			answer.setQuestionText( dto.getValue() );

			if ( dto.getCategoryStageName() != null ) {
				Stage stage = stageMap.get( dto.getCategoryStageName() );
				answer.setStageId( stage != null ? stage.getId() : null );
			}

			if ( dto.getRoleIds() != null ) {
				List<Long> validRoleIds = dto.getRoleIds().stream()
						.map( rolesById::get )
						.filter( Objects::nonNull )
						.map( Role::getId )
						.collect( Collectors.toList() );
				answer.setRoleIds( validRoleIds );
			}

			return answer;
		} ).collect( Collectors.toList() );
	}

	private void createInitialResponses( Project project,
			Questionnaire questionnaire,
			List<QuestionnaireResponseDocument.AnswerDocument> answerTemplate ) {

		Set<Representative> representatives = Optional.ofNullable( project.getRepresentatives() ).orElse( Collections.emptySet() );

		if ( representatives.isEmpty() ) {
			return;
		}

		List<QuestionnaireResponseDocument> responseDocuments = representatives.stream().map( rep -> {
			QuestionnaireResponseDocument response = new QuestionnaireResponseDocument();
			response.setProjectId( project.getId() );
			response.setQuestionnaireId( questionnaire.getId() );
			response.setRepresentativeId( rep.getId() );
			response.setStageId( questionnaire.getStage() != null ? questionnaire.getStage().getId() : null );
			response.setStatus( QuestionnaireResponseStatus.PENDING );
			response.setAnswers( cloneAnswerTemplate( answerTemplate ) );
			return response;
		} ).collect( Collectors.toList() );

		questionnaireResponseRepository.saveAll( responseDocuments );
	}

	private List<QuestionnaireResponseDocument.AnswerDocument> cloneAnswerTemplate( List<QuestionnaireResponseDocument.AnswerDocument> template ) {
		if ( template == null || template.isEmpty() ) {
			return Collections.emptyList();
		}

		List<QuestionnaireResponseDocument.AnswerDocument> clones = new ArrayList<>();
		for ( QuestionnaireResponseDocument.AnswerDocument original : template ) {
			QuestionnaireResponseDocument.AnswerDocument copy = new QuestionnaireResponseDocument.AnswerDocument();
			copy.setQuestionId( original.getQuestionId() );
			copy.setQuestionText( original.getQuestionText() );
			copy.setStageId( original.getStageId() );
			copy.setRoleIds( new ArrayList<>( Optional.ofNullable( original.getRoleIds() ).orElse( Collections.emptyList() ) ) );
			copy.setResponse( null );
			copy.setJustification( null );
			clones.add( copy );
		}
		return clones;
	}
}
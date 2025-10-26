package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import util.ModelMapperUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionnaireService {

	private final QuestionnaireRepository questionnaireRepository;
	private final QuestionRepository questionRepository;
	private final RoleRepository roleRepository;

	@Transactional( propagation = Propagation.MANDATORY )
	public void createQuestionnaires( Project project, Set<QuestionnaireDTO> questionnaireDTOs, Map<String, Stage> stageMap, Map<String, Iteration> iterationMap ) {

		if ( questionnaireDTOs == null || questionnaireDTOs.isEmpty() ) {
			return;
		}

		Map<String, Role> roleMap = roleRepository.findAll().stream().collect( Collectors.toMap( Role::getName, role -> role, ( r1, r2 ) -> r1 ) );

		for ( QuestionnaireDTO qnDto : questionnaireDTOs ) {

			Questionnaire questionnaire = ModelMapperUtils.map( qnDto, Questionnaire.class );

			questionnaire.setProject( project );

			if ( qnDto.getStageName() != null ) {
				questionnaire.setStage( stageMap.get( qnDto.getStageName() ) );
			} else if ( qnDto.getIterationName() != null ) {
				questionnaire.setIterationRef( iterationMap.get( qnDto.getIterationName() ) );
			}

			Questionnaire savedQuestionnaire = questionnaireRepository.save( questionnaire );

			if ( qnDto.getQuestions() != null && !qnDto.getQuestions().isEmpty() ) {
				List<Question> questions = createQuestionsForQuestionnaire( qnDto.getQuestions(), savedQuestionnaire, stageMap, roleMap );
				questionRepository.saveAll( questions );
			}
		}
	}

	private List<Question> createQuestionsForQuestionnaire( Set<QuestionDTO> questionDTOs, Questionnaire parentQuestionnaire, Map<String, Stage> stageMap, Map<String, Role> roleMap ) {

		return questionDTOs.stream().map( dto -> {
			Question question = new Question();
			question.setValue( dto.getValue() );

			question.setQuestionnaire( parentQuestionnaire );

			if ( dto.getCategoryStageName() != null ) {
				question.setStage( stageMap.get( dto.getCategoryStageName() ) );
			}

			if ( dto.getRoles() != null ) {
				Set<Role> targetRoles = dto.getRoles().stream().map( roleMap::get ).filter( Objects::nonNull ).collect( Collectors.toSet() );
				question.setRoles( targetRoles );
			}

			return question;
		} ).collect( Collectors.toList() );
	}
}
package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireReminderRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RoleRepository;
import com.ethicalsoft.ethicalsoft_complience.service.criteria.QuestionnaireReminderContext;
import com.ethicalsoft.ethicalsoft_complience.util.ModelMapperUtils;
import com.ethicalsoft.ethicalsoft_complience.util.ObjectUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireService {

	private final QuestionnaireRepository questionnaireRepository;
	private final RoleRepository roleRepository;
	private final QuestionnaireResponseRepository questionnaireResponseRepository;
	private final QuestionRepository questionRepository;
	private final EmailService emailService;

	@Transactional( propagation = Propagation.MANDATORY )
	public void createQuestionnaires( Project project, Set<QuestionnaireDTO> questionnaireDTOs, Map<String, Stage> stageMap, Map<String, Iteration> iterationMap ) {

		try {
			log.info("[questionnaire] Criando questionários para projeto id={} quantidade={}", project != null ? project.getId() : null, questionnaireDTOs != null ? questionnaireDTOs.size() : 0);
			if ( questionnaireDTOs == null || questionnaireDTOs.isEmpty() ) {
				return;
			}

			Map<Long, Role> rolesById = roleRepository.findAll().stream()
					.collect( Collectors.toMap( Role::getId, role -> role ) );

			for ( QuestionnaireDTO qnDto : questionnaireDTOs ) {

				Questionnaire questionnaire = ModelMapperUtils.map( qnDto, Questionnaire.class );

				questionnaire.setProject( project );

				boolean isWaterfall = ProjectTypeEnum.CASCATA.equals( project.getType() );
				boolean isIterative = ProjectTypeEnum.ITERATIVO.equals( project.getType() );

				if ( isWaterfall ) {
					Stage stage = stageMap.get( qnDto.getStageName() );
					questionnaire.setStage( stage );
					questionnaire.setIterationRef( null );
				} else if ( isIterative ) {
					Iteration iteration = iterationMap.get( qnDto.getIterationName() );
					questionnaire.setIterationRef( iteration );
					questionnaire.setStage( null );
				}

				Questionnaire savedQuestionnaire = questionnaireRepository.save( questionnaire );

				List<Question> persistedQuestions = createQuestions( qnDto.getQuestions(), project, savedQuestionnaire, stageMap, rolesById );
				savedQuestionnaire.setQuestions( new HashSet<>( persistedQuestions ) );

            // revisar a partir daqui
				List<QuestionnaireResponse.AnswerDocument> answerTemplate = buildAnswerTemplate( persistedQuestions );
				createInitialResponses( project, savedQuestionnaire, answerTemplate );
			}
			log.info("[questionnaire] Questionários criados para projeto id={}", project != null ? project.getId() : null);
		} catch ( Exception ex ) {
			log.error("[questionnaire] Falha ao criar questionários para projeto id={}", project != null ? project.getId() : null, ex);
			throw ex;
		}
	}

	@Transactional( propagation = Propagation.MANDATORY )
	public void createResponsesForRepresentative( Project project, Representative representative ) {
		try {
			log.info("[questionnaire] Gerando respostas base para representante id={} projeto={} ", representative != null ? representative.getId() : null, project != null ? project.getId() : null);
			Set<Questionnaire> questionnaires = Optional.ofNullable( project.getQuestionnaires() ).orElse( Collections.emptySet() );
			if ( questionnaires.isEmpty() ) {
				return;
			}

			for ( Questionnaire questionnaire : questionnaires ) {
				List<QuestionnaireResponse.AnswerDocument> template = buildAnswerTemplate( Optional.ofNullable( questionnaire.getQuestions() ).map( ArrayList::new ).orElse( new ArrayList<>() ) );
				QuestionnaireResponse response = new QuestionnaireResponse();
				response.setProjectId( project.getId() );
				response.setQuestionnaireId( questionnaire.getId() );
				response.setRepresentativeId( representative.getId() );
				response.setStageId( questionnaire.getStage() != null ? questionnaire.getStage().getId() : null );
				response.setStatus( QuestionnaireResponseStatus.PENDING );
				response.setAnswers( cloneAnswerTemplate( template ) );
				questionnaireResponseRepository.save( response );
			}
			log.info("[questionnaire] Questionários clonados para representante {}", representative != null ? representative.getId() : null);
		} catch ( Exception ex ) {
			log.error("[questionnaire] Falha ao gerar respostas para representante {}", representative != null ? representative.getId() : null, ex);
			throw ex;
		}
	}

	private List<Question> createQuestions( Set<QuestionDTO> questionDTOs,
			Project project,
			Questionnaire questionnaire,
			Map<String, Stage> stageMap,
			Map<Long, Role> rolesById ) {

		if (ObjectUtil.isNullOrEmpty(questionDTOs) ) {
			throw new BusinessException("É necessário ao menos uma questão para configurar o questionário '" + questionnaire.getName() + "'." );
		}

		return questionDTOs.stream().map( dto -> {
			String rawValue = Optional.ofNullable( dto.getValue() ).map( String::trim ).orElse( null );
			if ( ObjectUtil.isNullOrEmpty(rawValue) ) {
				throw new BusinessException( "Questão sem texto detectada ao configurar o questionário '" + questionnaire.getName() + "'. Informe um texto válido." );
			}

			Question question = new Question();
			question.setQuestionnaire( questionnaire );
			question.setValue( rawValue );

			boolean isWaterfall = ProjectTypeEnum.CASCATA.equals( project.getType() );
			if ( isWaterfall ) {
				Stage targetStage = dto.getCategoryStageName() != null ? stageMap.get( dto.getCategoryStageName() ) : questionnaire.getStage();
				question.setStages( Collections.singleton(targetStage) );
			} else {
				Set<Stage> mappedStages = Optional.ofNullable( dto.getStageNames() )
						.orElse( Collections.emptyList() )
						.stream()
						.map( stageMap::get )
						.filter( Objects::nonNull )
						.collect( Collectors.toSet() );
				question.setStages( mappedStages );
			}

			if (ObjectUtil.isNotNullAndNotEmpty(dto.getRoleIds()) ) {
				Set<Role> mappedRoles = dto.getRoleIds().stream()
						.map( rolesById::get )
						.filter( Objects::nonNull )
						.collect( Collectors.toSet() );
				question.setRoles( mappedRoles );
			}

			return question;
		} ).map( questionRepository::save ).toList();
	}

	private List<QuestionnaireResponse.AnswerDocument> buildAnswerTemplate(List<Question> persistedQuestions ) {
		if ( persistedQuestions == null || persistedQuestions.isEmpty() ) {
			return Collections.emptyList();
		}

		return persistedQuestions.stream().map( question -> {
			QuestionnaireResponse.AnswerDocument answer = new QuestionnaireResponse.AnswerDocument();
			answer.setQuestionId( question.getId().longValue() );
			answer.setQuestionText( question.getValue() );
			answer.setStageIds( Optional.ofNullable( question.getStages() )
					.orElse( Collections.emptySet() )
					.stream()
					.map( Stage::getId )
					.toList() );
			if ( question.getRoles() != null ) {
				answer.setRoleIds( question.getRoles().stream().map( Role::getId ).collect( Collectors.toList() ) );
			}
			return answer;
		} ).collect( Collectors.toList() );
	}

	private void createInitialResponses( Project project,
			Questionnaire questionnaire,
			List<QuestionnaireResponse.AnswerDocument> answerTemplate ) {

		Set<Representative> representatives = Optional.ofNullable( project.getRepresentatives() ).orElse( Collections.emptySet() );

		if ( representatives.isEmpty() ) {
			return;
		}

		List<QuestionnaireResponse> responseDocuments = representatives.stream().map(rep -> {
			QuestionnaireResponse response = new QuestionnaireResponse();
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

	private List<QuestionnaireResponse.AnswerDocument> cloneAnswerTemplate(List<QuestionnaireResponse.AnswerDocument> template ) {
		if ( template == null || template.isEmpty() ) {
			return Collections.emptyList();
		}

		List<QuestionnaireResponse.AnswerDocument> clones = new ArrayList<>();
		for ( QuestionnaireResponse.AnswerDocument original : template ) {
			QuestionnaireResponse.AnswerDocument copy = new QuestionnaireResponse.AnswerDocument();
			copy.setQuestionId( original.getQuestionId() );
			copy.setQuestionText( original.getQuestionText() );
			copy.setStageIds( original.getStageIds() != null ? new ArrayList<>( original.getStageIds() ) : null );
			copy.setRoleIds( new ArrayList<>( Optional.ofNullable( original.getRoleIds() ).orElse( Collections.emptyList() ) ) );
			copy.setResponse( null );
			copy.setJustification( null );
			copy.setAttachments( new ArrayList<>() );
			clones.add( copy );
		}
		return clones;
	}

	@Transactional(readOnly = true)
	public void sendQuestionnaireReminder(Long projectId, Integer questionnaireId, QuestionnaireReminderRequestDTO request) {
		try {
			log.info("[questionnaire] Enviando lembrete manual questionário={} projeto={} emails={}", questionnaireId, projectId, request != null ? request.emails() : null);
			Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
					.orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));
			if (!Objects.equals(questionnaire.getProject().getId(), projectId)) {
				throw new BusinessException("Questionário não pertence ao projeto informado");
			}
			validateQuestionnaireInProgress(questionnaire);
			Set<String> targetEmails = resolveEmailsForReminder(questionnaire.getProject(), questionnaire.getId(), request);
			QuestionnaireReminderContext context = QuestionnaireReminderContext.from(questionnaire);
			sendReminderEmails(targetEmails, context);
		} catch ( Exception ex ) {
			log.error("[questionnaire] Falha ao enviar lembrete questionário={} projeto={}", questionnaireId, projectId, ex);
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public void sendAutomaticQuestionnaireReminder(Questionnaire questionnaire) {
		if (!isQuestionnaireInProgress(questionnaire)) {
			log.info("[questionnaire] Ignorando lembrete automático para questionário {} fora da vigência", questionnaire.getId());
			return;
		}
		Set<String> emails = resolveEmailsForReminder(questionnaire.getProject(), questionnaire.getId(), null);
		if (emails.isEmpty()) {
			log.info("[questionnaire] Nenhum email pendente para lembrete automático questionário {}", questionnaire.getId());
			return;
		}
		QuestionnaireReminderContext context = QuestionnaireReminderContext.from(questionnaire);
		sendReminderEmails(emails, context);
	}

	private void validateQuestionnaireInProgress(Questionnaire questionnaire) {
		if (!isQuestionnaireInProgress(questionnaire)) {
			throw new BusinessException("Só é possível enviar lembretes para questionários em andamento.");
		}
	}

	private boolean isQuestionnaireInProgress(Questionnaire questionnaire) {
		LocalDate today = LocalDate.now();
		boolean withinDates = questionnaire.getApplicationStartDate() != null
				&& questionnaire.getApplicationEndDate() != null
				&& !today.isBefore(questionnaire.getApplicationStartDate())
				&& !today.isAfter(questionnaire.getApplicationEndDate());
		return withinDates && questionnaire.getStatus() == TimelineStatusEnum.EM_ANDAMENTO;
	}

	private Set<String> resolveEmailsForReminder(Project project, Integer questionnaireId, QuestionnaireReminderRequestDTO request) {
		Set<String> manualEmails = request != null && request.emails() != null
				? request.emails().stream().filter(Objects::nonNull).collect(Collectors.toSet())
				: Set.of();
		if (!manualEmails.isEmpty()) {
			return manualEmails;
		}
		List<QuestionnaireResponse> pendingResponses = questionnaireResponseRepository.findPendingResponses(project.getId(), questionnaireId);
		Set<Long> pendingRepresentativeIds = pendingResponses.stream()
				.map(QuestionnaireResponse::getRepresentativeId)
				.collect(Collectors.toSet());
		return Optional.ofNullable(project.getRepresentatives()).orElse(Set.of())
				.stream()
				.filter(rep -> pendingRepresentativeIds.contains(rep.getId()))
				.map(rep -> rep.getUser().getEmail())
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private void sendReminderEmails(Collection<String> emails, QuestionnaireReminderContext context) {
		emails.stream()
				.distinct()
				.forEach(email -> emailService.sendQuestionnaireReminderEmail(email, context));
	}
}

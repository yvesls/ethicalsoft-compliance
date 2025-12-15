package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.QuestionnaireSearchFilter;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.*;
import com.ethicalsoft.ethicalsoft_complience.util.ModelMapperUtils;
import com.ethicalsoft.ethicalsoft_complience.util.ObjectUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final RepresentativeRepository representativeRepository;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final QuestionnaireRepository questionnaireRepository;
	private final AuthService authService;
	private final EmailService emailService;
	private final QuestionnaireResponseRepository questionnaireResponseRepository;
	private final TimelineStatusService timelineStatusService;
	private final QuestionnaireService questionnaireService;

	@Transactional
	public Project createProjectShell( ProjectCreationRequestDTO request ) {
		try {
			log.info("[project] Criando casca de projeto nome={} deadline={}",
					request != null ? request.getName() : null,
					request != null ? request.getDeadline() : null);

			Project project = ModelMapperUtils.map( request, Project.class );
			project.setOwner( authService.getAuthenticatedUser() );

			project.setType( ProjectTypeEnum.fromValue(request != null ? request.getType() : null) );
			project.setStages( new HashSet<>() );
			project.setIterations( new HashSet<>() );
			project.setRepresentatives( new HashSet<>() );
			project.setQuestionnaires( new HashSet<>() );
			if ( project.getStatus() == null ) {
				project.setStatus( ProjectStatusEnum.RASCUNHO );
			}
			project.setTimelineStatus( TimelineStatusEnum.PENDENTE );
			project.setCurrentSituation( null );
			Project saved = projectRepository.save( project );
			log.info("[project] Projeto criado id={} status={}", saved.getId(), saved.getStatus());
			return saved;
		} catch ( Exception ex ) {
			log.error("[project] Falha ao criar casca de projeto nome={}", request != null ? request.getName() : null, ex);
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public Page<ProjectSummaryResponseDTO> getAllProjectSummaries( ProjectSearchRequestDTO filters, Pageable pageable) {

        Specification<Project> spec = ProjectSpecification.findByCriteria(filters, authService.getAuthenticatedUser());

        Page<Project> projectPage = projectRepository.findAll(spec, pageable);

        LocalDate now = LocalDate.now();

        return projectPage.map(project -> {
			String currentStage = null;
			Integer currentIteration = null;

			if (project.getType() == ProjectTypeEnum.CASCATA) {
				currentStage = findCurrentStageName(project.getQuestionnaires(), now);
			} else if (project.getType() == ProjectTypeEnum.ITERATIVO) {
				currentIteration = findCurrentIterationNumber(project.getIterations(), now);
			}

			return ProjectSummaryResponseDTO.builder()
					.id(project.getId())
					.name(project.getName())
					.type(project.getType().name())
					.status(project.getStatus())
					.timelineStatus(project.getTimelineStatus())
					.deadline( project.getDeadline() )
					.startDate(project.getStartDate())
					.representativeCount(project.getRepresentatives() != null ? project.getRepresentatives().size() : 0)
					.stageCount(project.getStages() != null ? project.getStages().size() : 0)
					.iterationCount(project.getIterations() != null ? project.getIterations().size() : 0)
					.currentStage(currentStage)
					.currentIteration(currentIteration)
					.currentSituation(project.getCurrentSituation())
					.build();
		});
	}

	private String buildCurrentSituation(Project project, String currentStage, Integer currentIteration) {
		if (project.getType() == ProjectTypeEnum.CASCATA) {
			return currentStage;
		}
		if (project.getType() == ProjectTypeEnum.ITERATIVO && currentIteration != null && project.getIterationCount() != null) {
			return "Sprint " + currentIteration + "/" + project.getIterationCount();
		}
		return null;
	}

	private String findCurrentStageName(Set<Questionnaire> questionnaires, LocalDate now) {
		if (questionnaires == null || questionnaires.isEmpty()) {
			return null;
		}
		return questionnaires.stream()
				.filter(q -> q.getApplicationStartDate() != null && q.getApplicationEndDate() != null &&
						!now.isBefore(q.getApplicationStartDate()) && !now.isAfter(q.getApplicationEndDate()))
				.map(Questionnaire::getStage)
				.filter( Objects::nonNull)
				.map(Stage::getName)
				.findFirst()
				.orElse(null);
	}

	private Integer findCurrentIterationNumber(Set<Iteration> iterations, LocalDate now) {
		if (iterations == null || iterations.isEmpty()) {
			return null;
		}

		var sortedIterations = iterations.stream()
				.filter(it -> it.getApplicationStartDate() != null)
				.sorted( Comparator.comparing(Iteration::getApplicationStartDate))
				.toList();

		for (int i = 0; i < sortedIterations.size(); i++) {
			Iteration it = sortedIterations.get(i);
			if (it.getApplicationEndDate() != null &&
					!now.isBefore(it.getApplicationStartDate()) && !now.isAfter(it.getApplicationEndDate())) {
				return i + 1;
			}
		}
		return null;
	}

	@Transactional
	public Set<Representative> createRepresentatives( Project project, Set<RepresentativeDTO> repDTOs ) {
		try {
			log.info("[project] Criando representantes para projeto id={} quantidade={}", project != null ? project.getId() : null, repDTOs != null ? repDTOs.size() : 0);
			if (ObjectUtil.isNullOrEmpty( repDTOs ) ) {
				return new HashSet<>();
			}

            if ( ObjectUtil.isNullOrEmpty( project ) ) {
                throw new BusinessException("Projeto inválido para criação de representantes.");
            }

			Set<Long> requestedRoleIds = repDTOs.stream()
					.filter( dto -> dto.getRoleIds() != null )
					.flatMap( dto -> dto.getRoleIds().stream() )
					.collect( Collectors.toSet() );

			Map<Long, Role> resolvedRoles = requestedRoleIds.isEmpty()
					? Map.of()
					: roleRepository.findAllById( requestedRoleIds ).stream()
					.collect( Collectors.toMap( Role::getId, Function.identity() ) );

			if ( resolvedRoles.size() != requestedRoleIds.size() ) {
				throw new EntityNotFoundException( "Um ou mais papéis (Roles) não foram encontrados." );
			}

			User currentAdmin = authService.getAuthenticatedUser();

			Set<Representative> representatives = repDTOs.stream().map( dto -> {
				UserResolutionResult resolution = resolveOrCreateUser( dto );
				Set<Role> roles = mapRoles( dto.getRoleIds(), resolvedRoles );

				Representative rep = new Representative();
				rep.setProject( project );
				rep.setUser( resolution.user() );
				rep.setRoles( roles );
				rep.setWeight( dto.getWeight() );
				rep.setCreationDate( LocalDate.now() );

				representativeRepository.save( rep );
				questionnaireService.createResponsesForRepresentative( project, rep );

				resolution.temporaryPassword().ifPresent( tempPassword ->
					emailService.sendNewUserCredentialsEmail(
							rep.getUser().getEmail(),
							rep.getUser().getFirstName(),
							tempPassword,
							rep.getProject().getName(),
							currentAdmin.getFirstName() + " " + currentAdmin.getLastName()
					)
				);

				emailService.sendProjectAssignmentEmail(
						rep.getUser().getEmail(),
						rep.getUser().getFirstName(),
						project.getName(),
						project.getId(),
						currentAdmin.getFirstName() + " " + currentAdmin.getLastName(),
						currentAdmin.getEmail(),
						rep.getRoles().stream().map( Role::getName ).toList(),
						project.getCurrentSituation(),
						project.getStartDate(),
						project.getDeadline(),
						findNextQuestionnaireDate( project )
				);

				return rep;
			} ).collect( Collectors.toSet() );

			log.info("[project] {} representantes vinculados ao projeto id={} ", representatives.size(), project.getId());
			return representatives;
		} catch ( Exception ex ) {
			log.error("[project] Falha ao criar representantes para projeto id={}", project != null ? project.getId() : null, ex);
			throw ex;
		}
	}

	private UserResolutionResult resolveOrCreateUser( RepresentativeDTO dto ) {
		if ( StringUtils.hasText( dto.getEmail() ) ) {
			return userRepository.findByEmail( dto.getEmail() )
					.map( user -> new UserResolutionResult( user, Optional.empty() ) )
					.orElseGet( () -> createUserFromDto( dto ) );
		}

		throw new IllegalArgumentException( "Representante precisa informar userId ou email." );
	}

	private UserResolutionResult createUserFromDto( RepresentativeDTO dto ) {
		if ( !StringUtils.hasText( dto.getEmail() ) || !StringUtils.hasText( dto.getFirstName() ) || !StringUtils.hasText( dto.getLastName() ) ) {
			throw new IllegalArgumentException( "Dados insuficientes para criar um usuário." );
		}

		String tempPassword = generateTempPassword();

		User user = new User();
        user.setRole(UserRoleEnum.USER);
		user.setFirstName( dto.getFirstName() );
		user.setLastName( dto.getLastName() );
		user.setEmail( dto.getEmail() );
		user.setPassword( passwordEncoder.encode( tempPassword ) );
		user.setFirstAccess( true );
		user.setAcceptedTerms( false );

		user = userRepository.save( user );

		return new UserResolutionResult( user, Optional.of( tempPassword ) );
	}

	private String generateTempPassword() {
        return UUID.randomUUID().toString().replace( "-", "" ).substring( 0, 12 );
    }

	private Set<Role> mapRoles( Set<Long> desiredRoleIds, Map<Long, Role> resolvedRoles ) {
		if ( desiredRoleIds == null || desiredRoleIds.isEmpty() ) {
			return Set.of();
		}

		return desiredRoleIds.stream()
				.map( resolvedRoles::get )
				.collect( Collectors.toSet() );
	}

	@Transactional(readOnly = true)
	public List<RoleSummaryResponseDTO> listRoleSummaries() {
		try {
			log.debug("[project] Listando roles cadastros");
			List<RoleSummaryResponseDTO> roles = roleRepository.findAll().stream()
					.map(role -> new RoleSummaryResponseDTO(role.getId(), role.getName()))
					.collect(toList());
			log.info("[project] {} roles retornados", roles.size());
			return roles;
		} catch ( Exception ex ) {
			log.error("[project] Falha ao listar roles", ex);
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public ProjectDetailResponseDTO getProjectById(Long projectId) {
        try {
            log.info("[project] Buscando detalhes do projeto id={}", projectId);
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

            assertUserCanAccessProject(project);

            int representativeCount = project.getRepresentatives() != null ? project.getRepresentatives().size() : 0;
            int stageCount = project.getStages() != null ? project.getStages().size() : 0;
            int iterationCount = project.getIterations() != null ? project.getIterations().size() : 0;
            LocalDate now = LocalDate.now();
            String currentStage = null;
            Integer currentIteration = null;

            if (project.getType() == ProjectTypeEnum.CASCATA) {
                currentStage = findCurrentStageName(project.getQuestionnaires(), now);
            } else if (project.getType() == ProjectTypeEnum.ITERATIVO) {
                currentIteration = findCurrentIterationNumber(project.getIterations(), now);
            }

            project.setCurrentSituation( buildCurrentSituation( project, currentStage, currentIteration) );

            return ProjectDetailResponseDTO.builder()
                    .id(project.getId())
                    .name(project.getName())
                    .type(project.getType() != null ? project.getType().name() : null)
                    .startDate(project.getStartDate())
                    .deadline(project.getDeadline())
                    .closingDate(project.getClosingDate())
                    .status(project.getStatus())
                    .timelineStatus(project.getTimelineStatus())
                    .iterationDuration(project.getIterationDuration())
                    .configuredIterationCount(project.getIterationCount())
                    .representativeCount(representativeCount)
                    .stageCount(stageCount)
                    .iterationCount(iterationCount)
                    .currentStage(currentStage)
                    .currentIteration(currentIteration)
                    .currentSituation(project.getCurrentSituation())
                    .build();
        } catch ( Exception ex ) {
            log.error("[project] Falha ao buscar projeto id={}", projectId, ex);
            throw ex;
        }
    }

    private void assertUserCanAccessProject(Project project) {
        User currentUser = authService.getAuthenticatedUser();
        if (!UserRoleEnum.ADMIN.equals(currentUser.getRole()) && project.getRepresentatives().stream().noneMatch(rep -> rep.getUser().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("Usuário não possui acesso ao projeto " + project.getId());
        }
    }

    @Transactional(readOnly = true)
    public Page<QuestionnaireSummaryResponseDTO> listQuestionnaires(Long projectId, Pageable pageable, QuestionnaireSearchFilter filter) {
		try {
			log.info("[project] Listando questionários do projeto id={} filtroNome={}", projectId, filter != null ? filter.name() : null);
			Project project = projectRepository.findById(projectId)
					.orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));
			assertUserCanAccessProject(project);

			Set<Representative> projectRepresentatives = Optional.ofNullable(project.getRepresentatives()).orElse(Set.of());
			Map<Long, Representative> representativesById = projectRepresentatives.stream()
					.collect(Collectors.toMap(Representative::getId, rep -> rep));

			Specification<Questionnaire> spec = Specification.where((root, query, cb) -> cb.equal(root.get("project").get("id"), projectId));
			if (filter != null) {
				if (StringUtils.hasText(filter.name())) {
					spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
				}
				if (StringUtils.hasText(filter.stageName())) {
					spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("stage").get("name")), filter.stageName().toLowerCase()));
				}
				if (StringUtils.hasText(filter.iterationName())) {
					spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("iterationRef").get("name")), filter.iterationName().toLowerCase()));
				}
			}

			Page<QuestionnaireSummaryResponseDTO> page = questionnaireRepository.findAll(spec, pageable)
					.map(questionnaire -> buildQuestionnaireSummary(questionnaire, representativesById));
			log.info("[project] {} questionários retornados para projeto {}", page.getNumberOfElements(), projectId);
			return page;
		} catch ( Exception ex ) {
			log.error("[project] Falha ao listar questionários do projeto id={}", projectId, ex);
			throw ex;
		}
	}

    private QuestionnaireSummaryResponseDTO buildQuestionnaireSummary(Questionnaire questionnaire,
                                                                      Map<Long, Representative> representativesById) {
        List<QuestionnaireResponse> responses = questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireId(questionnaire.getProject().getId(), questionnaire.getId());

        Map<Long, QuestionnaireResponse> responseByRep = responses.stream()
                .filter(resp -> resp.getRepresentativeId() != null)
                .collect(Collectors.toMap(QuestionnaireResponse::getRepresentativeId, r -> r, (a, b) -> a));

        int totalRespondents = representativesById.size();
        AtomicInteger responded = new AtomicInteger();
        AtomicReference<LocalDateTime> lastResponseAt = new AtomicReference<>();
        List<RespondentStatusDTO> respondentStatus = representativesById.values().stream()
                .map(rep -> {
                    QuestionnaireResponse response = responseByRep.get(rep.getId());
                    QuestionnaireResponseStatus status = response != null ? response.getStatus() : QuestionnaireResponseStatus.PENDING;
                    LocalDateTime completedAt = response != null ? response.getSubmissionDate() : null;
                    if (status == QuestionnaireResponseStatus.COMPLETED) {
                        responded.getAndIncrement();
                        if (completedAt != null && (lastResponseAt.get() == null || completedAt.isAfter(lastResponseAt.get()))) {
                            lastResponseAt.set(completedAt);
                        }
                    }
                    return RespondentStatusDTO.builder()
                            .representativeId(rep.getId())
                            .name(rep.getUser().getFirstName() + " " + rep.getUser().getLastName())
                            .email(rep.getUser().getEmail())
                            .status(status)
                            .completedAt(completedAt)
                            .build();
                }).collect(toList());

        int pending = Math.max(totalRespondents - responded.get(), 0);
        QuestionnaireResponseStatus progressStatus;
        if (responded.get() == 0) {
            progressStatus = QuestionnaireResponseStatus.PENDING;
        } else if (responded.get() < totalRespondents) {
            progressStatus = QuestionnaireResponseStatus.IN_PROGRESS;
        } else {
            progressStatus = QuestionnaireResponseStatus.COMPLETED;
        }

        return QuestionnaireSummaryResponseDTO.builder()
                .id(questionnaire.getId())
                .name(questionnaire.getName())
                .applicationStartDate(questionnaire.getApplicationStartDate())
                .applicationEndDate(questionnaire.getApplicationEndDate())
                .stageName(questionnaire.getStage() != null ? questionnaire.getStage().getName() : null)
                .iterationName(questionnaire.getIterationRef() != null ? questionnaire.getIterationRef().getName() : null)
                .totalRespondents(totalRespondents)
                .respondedRespondents(responded.get())
                .pendingRespondents(pending)
                .lastResponseAt(lastResponseAt.get())
                .progressStatus(progressStatus)
				.status(questionnaire.getStatus())
				.respondents(respondentStatus)
                .build();
    }

	record UserResolutionResult( User user, Optional<String> temporaryPassword ) {}

	@Transactional(readOnly = true)
	public Project reloadProjectAggregate( Long projectId ) {
		try {
			return projectRepository.findByIdWithDetails( projectId )
                    .orElseThrow( () -> new EntityNotFoundException( "Projeto no encontrado: " + projectId ) );
		} catch ( Exception ex ) {
			log.error("[project] Falha ao recarregar agregado do projeto id={}", projectId, ex);
			throw ex;
		}
	}

	@Transactional
	public Project refreshTimelineStatus( Long projectId ) {
		try {
			log.info("[project] Atualizando timeline do projeto id={}", projectId);
			Project project = projectRepository.findByIdWithDetails( projectId )
					.orElseThrow( () -> new EntityNotFoundException( "Projeto no encontrado: " + projectId ) );
			timelineStatusService.updateProjectTimeline( project );
			Project saved = projectRepository.save( project );
			log.info("[project] Timeline recalculada para projeto {} com status {}", projectId, saved.getTimelineStatus());
			return saved;
		} catch ( Exception ex ) {
			log.error("[project] Falha ao atualizar timeline do projeto id={}", projectId, ex);
			throw ex;
		}
	}

	private LocalDate findNextQuestionnaireDate( Project project ) {
		return project.getQuestionnaires().stream()
				.map( Questionnaire::getApplicationStartDate )
				.filter( Objects::nonNull )
				.filter( date -> !date.isBefore( LocalDate.now() ) )
				.sorted()
				.findFirst()
				.orElse( project.getStartDate() );
	}
}

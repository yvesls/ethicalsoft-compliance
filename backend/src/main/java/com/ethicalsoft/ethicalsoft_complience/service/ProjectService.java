package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponseDocument;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.*;
import com.ethicalsoft.ethicalsoft_complience.util.ModelMapperUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

	@Transactional
	public Project createProjectShell( ProjectCreationRequestDTO request ) {
		Project project = ModelMapperUtils.map( request, Project.class );
		project.setType( ProjectTypeEnum.fromValue( request.getType() ) );
		project.setStages( new HashSet<>() );
		project.setIterations( new HashSet<>() );
		project.setRepresentatives( new HashSet<>() );
		project.setQuestionnaires( new HashSet<>() );
		return projectRepository.save( project );
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
					.deadline( project.getDeadline() )
					.startDate(project.getStartDate())
					.representativeCount(project.getRepresentatives() != null ? project.getRepresentatives().size() : 0)
					.stageCount(project.getStages() != null ? project.getStages().size() : 0)
					.iterationCount(project.getIterations() != null ? project.getIterations().size() : 0)
					.currentStage(currentStage)
					.currentIteration(currentIteration)
					.build();
		});
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
		if ( repDTOs == null || repDTOs.isEmpty() ) {
			return new HashSet<>();
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

		Set<Representative> representatives = repDTOs.stream().map( dto -> {
			UserResolutionResult resolution = resolveOrCreateUser( dto );
			Set<Role> roles = mapRoles( dto.getRoleIds(), resolvedRoles );

			Representative rep = new Representative();
			rep.setProject( project );
			rep.setUser( resolution.user() );
			rep.setRoles( roles );
			rep.setWeight( dto.getWeight() );
			rep.setCreationDate( LocalDate.now() );

			resolution.temporaryPassword().ifPresent( tempPassword ->
				emailService.sendNewUserCredentialsEmail(
						rep.getUser().getEmail(),
						rep.getUser().getFirstName(),
						tempPassword
				)
			);

			return rep;
		} ).collect( Collectors.toSet() );

		return new HashSet<>( representativeRepository.saveAll( representatives ) );
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
		return roleRepository.findAll().stream()
				.map(role -> new RoleSummaryResponseDTO(role.getId(), role.getName()))
				.collect(toList());
	}

	@Transactional(readOnly = true)
	public ProjectDetailResponseDTO getProjectById(Long projectId) {
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

        return ProjectDetailResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .type(project.getType() != null ? project.getType().name() : null)
                .startDate(project.getStartDate())
                .deadline(project.getDeadline())
                .closingDate(project.getClosingDate())
                .status(project.getStatus())
                .iterationDuration(project.getIterationDuration())
                .configuredIterationCount(project.getIterationCount())
                .representativeCount(representativeCount)
                .stageCount(stageCount)
                .iterationCount(iterationCount)
                .currentStage(currentStage)
                .currentIteration(currentIteration)
                .build();
    }

    private void assertUserCanAccessProject(Project project) {
        User currentUser = authService.getAuthenticatedUser();
        if (!UserRoleEnum.ADMIN.equals(currentUser.getRole()) && project.getRepresentatives().stream().noneMatch(rep -> rep.getUser().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("Usuário não possui acesso ao projeto " + project.getId());
        }
    }

    @Transactional(readOnly = true)
    public Page<QuestionnaireSummaryResponseDTO> listQuestionnaires(Long projectId, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));
        assertUserCanAccessProject(project);

        Set<Representative> projectRepresentatives = Optional.ofNullable(project.getRepresentatives()).orElse(Set.of());
        Map<Long, Representative> representativesById = projectRepresentatives.stream()
                .collect(Collectors.toMap(Representative::getId, rep -> rep));

        return questionnaireRepository.findByProjectId(projectId, pageable)
                .map(questionnaire -> buildQuestionnaireSummary(questionnaire, representativesById));
    }

    private QuestionnaireSummaryResponseDTO buildQuestionnaireSummary(Questionnaire questionnaire,
                                                                      Map<Long, Representative> representativesById) {
        List<QuestionnaireResponseDocument> responses = questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireId(questionnaire.getProject().getId(), questionnaire.getId());

        Map<Long, QuestionnaireResponseDocument> responseByRep = responses.stream()
                .filter(resp -> resp.getRepresentativeId() != null)
                .collect(Collectors.toMap(QuestionnaireResponseDocument::getRepresentativeId, r -> r, (a, b) -> a));

        int totalRespondents = representativesById.size();
        AtomicInteger responded = new AtomicInteger();
        AtomicReference<LocalDateTime> lastResponseAt = new AtomicReference<>();
        List<RespondentStatusDTO> respondentStatus = representativesById.values().stream()
                .map(rep -> {
                    QuestionnaireResponseDocument response = responseByRep.get(rep.getId());
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
                }).collect(Collectors.toList());

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
                .respondents(respondentStatus)
                .build();
    }

	record UserResolutionResult( User user, Optional<String> temporaryPassword ) {}
}
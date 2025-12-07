package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.RoleSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RoleRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import com.ethicalsoft.ethicalsoft_complience.util.ModelMapperUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
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
	private final EmailService emailService;

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

		Specification<Project> spec = ProjectSpecification.findByCriteria(filters);

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

		User user = new User();
		user.setFirstName( dto.getFirstName() );
		user.setLastName( dto.getLastName() );
		user.setEmail( dto.getEmail() );
		user.setPassword( passwordEncoder.encode( UUID.randomUUID().toString() ) );
		user.setFirstAccess( true );
		user.setAcceptedTerms( false );

		user = userRepository.save( user );

		return new UserResolutionResult( user, Optional.of( user.getPassword() ) );
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

	record UserResolutionResult( User user, Optional<String> temporaryPassword ) {}
}
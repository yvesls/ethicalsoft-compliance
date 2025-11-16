package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectSummaryResponseDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final RepresentativeRepository representativeRepository;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

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

		// 1. Cria a query dinâmica (Specification) a partir dos filtros
		Specification<Project> spec = ProjectSpecification.findByCriteria(filters);

		// 2. Chama o NOVO método do repositório
		// Este método agora executa a query dinâmica (spec)
		// e o EntityGraph (para evitar N+1)
		Page<Project> projectPage = projectRepository.findAll(spec, pageable);

		LocalDate now = LocalDate.now();

		// O resto da lógica de mapeamento permanece idêntica
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

		Set<Representative> representatives = repDTOs.stream().map( dto -> {
			User user = userRepository.findById( dto.getUserId() ).orElseThrow( () -> new EntityNotFoundException( "Usuário não encontrado: " + dto.getUserId() ) );

			Set<Role> roles = new HashSet<>( roleRepository.findAllById( dto.getRoleIds() ) );
			if ( roles.size() != dto.getRoleIds().size() ) {
				throw new EntityNotFoundException( "Um ou mais papéis (Roles) não foram encontrados." );
			}

			Representative rep = new Representative();
			rep.setProject( project );
			rep.setUser( user );
			rep.setRoles( roles );
			rep.setWeight( dto.getWeight() );
			rep.setCreationDate( LocalDate.now() );
			return rep;
		} ).collect( Collectors.toSet() );

		return new HashSet<>( representativeRepository.saveAll( representatives ) );
	}
}
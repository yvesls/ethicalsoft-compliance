package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
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
	public Project createProjectShell( ProjectCreationRequest request ) {
		Project project = ModelMapperUtils.map( request, Project.class );

		project.setType( ProjectTypeEnum.fromValue( request.getType() ) );

		project.setStages( new HashSet<>() );
		project.setIterations( new HashSet<>() );
		project.setRepresentatives( new HashSet<>() );
		project.setQuestionnaires( new HashSet<>() );

		return projectRepository.save( project );
	}

	@Transactional(readOnly = true)
	public Page<ProjectSummaryResponseDTO> getAllProjectSummaries( Pageable pageable) {
		Page<Project> projectPage = projectRepository.findAll(pageable);

		return projectPage.map(project -> ProjectSummaryResponseDTO.builder()
				.id(project.getId())
				.name(project.getName())
				.type(project.getType().name())
				.startDate(project.getStartDate())
				.representativeCount(project.getRepresentatives() != null ? project.getRepresentatives().size() : 0)
				.stageCount(project.getStages() != null ? project.getStages().size() : 0)
				.iterationCount(project.getIterations() != null ? project.getIterations().size() : 0)
				.build());
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
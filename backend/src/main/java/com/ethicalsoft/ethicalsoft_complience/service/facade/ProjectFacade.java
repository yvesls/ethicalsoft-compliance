package com.ethicalsoft.ethicalsoft_complience.service.facade;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.ProjectResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.service.ProjectService;
import com.ethicalsoft.ethicalsoft_complience.service.strategy.ProjectCreationStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectFacade {

	private final ProjectService projectService;
	private final List<ProjectCreationStrategy> creationStrategies;
	private final Map<ProjectTypeEnum, ProjectCreationStrategy> strategyMap = new EnumMap<>( ProjectTypeEnum.class );

	@PostConstruct
	public void initStrategyMap() {
		creationStrategies.forEach( strategy -> strategyMap.put( strategy.getType(), strategy ) );
	}

	@Transactional
	public ProjectResponseDTO createProject(ProjectCreationRequestDTO request ) {

		Project project = projectService.createProjectShell( request );

		ProjectTypeEnum type = ProjectTypeEnum.fromValue( request.getType() );
		ProjectCreationStrategy strategy = strategyMap.get( type );

		if ( strategy == null ) {
			throw new IllegalArgumentException( "Tipo de projeto não suportado: " + request.getType() );
		}

		strategy.createStructure( project, request );

		Set<Representative> representatives = projectService.createRepresentatives( project, request.getRepresentatives() );

		Project refreshed = projectService.refreshTimelineStatus( project.getId() );

		return ProjectResponseDTO.builder()
				.id( refreshed.getId() )
				.name( refreshed.getName() )
				.type( refreshed.getType().name() )
				.startDate( refreshed.getStartDate() )
				.timelineStatus( refreshed.getTimelineStatus() )
				.currentSituation( refreshed.getCurrentSituation() )
				.representativeCount( representatives.size() )
				.stageCount( request.getStages() != null ? request.getStages().size() : 0 )
				.iterationCount( request.getIterations() != null ? request.getIterations().size() : 0 )
				.build();
	}
}
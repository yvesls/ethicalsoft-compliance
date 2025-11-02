package com.ethicalsoft.ethicalsoft_complience.service.strategy.project_strategy;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequest;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.IterationRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.service.QuestionnaireService;
import com.ethicalsoft.ethicalsoft_complience.service.strategy.ProjectCreationStrategy;
import com.ethicalsoft.ethicalsoft_complience.util.ModelMapperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IterativeProjectStrategy implements ProjectCreationStrategy {

	private final StageRepository stageRepository;
	private final IterationRepository iterationRepository;
	private final QuestionnaireService questionnaireService;

	@Override
	public ProjectTypeEnum getType() {
		return ProjectTypeEnum.ITERATIVO;
	}

	@Override
	public void createStructure( Project project, ProjectCreationRequest request ) {
		Map<String, Stage> stageMap = Collections.emptyMap();
		if ( request.getStages() != null && !request.getStages().isEmpty() ) {
			List<Stage> stages = ModelMapperUtils.mapAll( request.getStages(), Stage.class );
			stages.forEach( stage -> stage.setProject( project ) );
			List<Stage> savedStages = stageRepository.saveAll( stages );
			stageMap = savedStages.stream().collect( Collectors.toMap( Stage::getName, Function.identity() ) );
		}

		if ( request.getIterations() == null || request.getIterations().isEmpty() ) {
			throw new IllegalArgumentException( "Projetos Iterativos devem ter iterações definidas." );
		}
		List<Iteration> iterations = ModelMapperUtils.mapAll( request.getIterations(), Iteration.class );
		iterations.forEach( iteration -> iteration.setProject( project ) );
		List<Iteration> savedIterations = iterationRepository.saveAll( iterations );

		Map<String, Iteration> iterationMap = savedIterations.stream().collect( Collectors.toMap( Iteration::getName, Function.identity() ) );

		questionnaireService.createQuestionnaires( project, request.getQuestionnaires(), stageMap, iterationMap );
	}
}
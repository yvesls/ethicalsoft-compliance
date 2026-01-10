package com.ethicalsoft.ethicalsoft_complience.application.service.strategy.project_strategy;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.IterationCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.StageCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.ProjectQuestionnaireCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.service.strategy.ProjectCreationStrategy;
import com.ethicalsoft.ethicalsoft_complience.common.util.mapper.ModelMapperUtils;
import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
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

	private final StageCommandPort stageCommandPort;
	private final IterationCommandPort iterationCommandPort;
	private final ProjectQuestionnaireCommandPort projectQuestionnaireCommandPort;

	@Override
	public ProjectTypeEnum getType() {
		return ProjectTypeEnum.ITERATIVO;
	}

	@Override
	public void createStructure( Project project, ProjectCreationRequestDTO request ) {
		Map<String, Stage> stageMap = Collections.emptyMap();

		if (ObjectUtils.isNotNullAndNotEmpty(request.getStages()) ) {
			List<Stage> stages = ModelMapperUtils.mapAll( request.getStages(), Stage.class );

			stages.forEach( stage -> stage.setProject( project ) );
			List<Stage> savedStages = stageCommandPort.saveAll( stages );

			stageMap = savedStages.stream().collect( Collectors.toMap( Stage::getName, Function.identity() ) );
		}

		if ( ObjectUtils.isNullOrEmpty(request.getIterations()) ) {
			throw new IllegalArgumentException( "Projetos Iterativos devem ter iterações definidas." );
		}

		List<Iteration> iterations = ModelMapperUtils.mapAll( request.getIterations(), Iteration.class );
		iterations.forEach( iteration -> iteration.setProject( project ) );

		List<Iteration> savedIterations = iterationCommandPort.saveAll( iterations );

		Map<String, Iteration> iterationMap = savedIterations.stream().collect( Collectors.toMap( Iteration::getName, Function.identity() ) );

		projectQuestionnaireCommandPort.createQuestionnaires( project, request.getQuestionnaires(), stageMap, iterationMap );
	}
}
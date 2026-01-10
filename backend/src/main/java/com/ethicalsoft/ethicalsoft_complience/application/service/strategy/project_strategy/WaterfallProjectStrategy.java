package com.ethicalsoft.ethicalsoft_complience.application.service.strategy.project_strategy;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
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
public class WaterfallProjectStrategy implements ProjectCreationStrategy {

	private final StageCommandPort stageCommandPort;
	private final ProjectQuestionnaireCommandPort projectQuestionnaireCommandPort;

	@Override
	public ProjectTypeEnum getType() {
		return ProjectTypeEnum.CASCATA;
	}

	@Override
	public void createStructure( Project project, ProjectCreationRequestDTO request ) {
		if (ObjectUtils.isNullOrEmpty(request.getStages())) {
			throw new IllegalArgumentException( "Projetos Cascata devem ter etapas definidas." );
		}

		List<Stage> stages = ModelMapperUtils.mapAll( request.getStages(), Stage.class );

		stages.forEach( stage -> stage.setProject( project ) );

		List<Stage> savedStages = stageCommandPort.saveAll( stages );

		Map<String, Stage> stageMap = savedStages.stream().collect( Collectors.toMap( Stage::getName, Function.identity() ) );

		Map<String, Iteration> emptyIterationMap = Collections.emptyMap();

		projectQuestionnaireCommandPort.createQuestionnaires( project, request.getQuestionnaires(), stageMap, emptyIterationMap );
	}
}
package com.ethicalsoft.ethicalsoft_complience.service.strategy.project_strategy;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequest;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.service.QuestionnaireService;
import com.ethicalsoft.ethicalsoft_complience.service.strategy.ProjectCreationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import util.ModelMapperUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WaterfallProjectStrategy implements ProjectCreationStrategy {

	private final StageRepository stageRepository;
	private final QuestionnaireService questionnaireService;

	@Override
	public ProjectTypeEnum getType() {
		return ProjectTypeEnum.WATERFALL;
	}

	@Override
	public void createStructure( Project project, ProjectCreationRequest request ) {
		if ( request.getStages() == null || request.getStages().isEmpty() ) {
			throw new IllegalArgumentException( "Projetos Cascata devem ter etapas definidas." );
		}

		List<Stage> stages = ModelMapperUtils.mapAll( request.getStages(), Stage.class );

		stages.forEach( stage -> stage.setProject( project ) );

		List<Stage> savedStages = stageRepository.saveAll( stages );

		Map<String, Stage> stageMap = savedStages.stream().collect( Collectors.toMap( Stage::getName, Function.identity() ) );

		Map<String, Iteration> emptyIterationMap = Collections.emptyMap();

		questionnaireService.createQuestionnaires( project, request.getQuestionnaires(), stageMap, emptyIterationMap );
	}
}
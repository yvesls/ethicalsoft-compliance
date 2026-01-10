package com.ethicalsoft.ethicalsoft_complience.application.service.strategy;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;

public interface ProjectCreationStrategy {

	void createStructure( Project project, ProjectCreationRequestDTO request );

	ProjectTypeEnum getType();
}
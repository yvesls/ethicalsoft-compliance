package com.ethicalsoft.ethicalsoft_complience.service.strategy;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectCreationRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;

public interface ProjectCreationStrategy {

	void createStructure( Project project, ProjectCreationRequestDTO request );

	ProjectTypeEnum getType();
}
package com.ethicalsoft.ethicalsoft_complience.application.port.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.ProjectIsepResult;

public interface ProjectIsepResultCommandPort {

    ProjectIsepResult save(ProjectIsepResult result);
}


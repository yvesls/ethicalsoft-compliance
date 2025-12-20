package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import org.springframework.stereotype.Service;

@Service
public class ProjectSituationPolicy {

    public String buildCurrentSituation(Project project, String currentStage, Integer currentIteration) {
        if (project.getType() == ProjectTypeEnum.CASCATA) {
            return currentStage;
        }
        if (project.getType() == ProjectTypeEnum.ITERATIVO && currentIteration != null && project.getIterationCount() != null) {
            return "Sprint " + currentIteration + "/" + project.getIterationCount();
        }
        return null;
    }
}


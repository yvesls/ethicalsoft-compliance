package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;

public interface TemplateSummaryProjection {
    String getId();
    String getName();
    String getDescription();
    ProjectTypeEnum getType();
}


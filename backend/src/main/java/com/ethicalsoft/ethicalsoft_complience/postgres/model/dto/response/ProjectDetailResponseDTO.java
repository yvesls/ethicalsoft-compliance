package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectStatusEnum;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class ProjectDetailResponseDTO {
    Long id;
    String name;
    String type;
    LocalDate startDate;
    LocalDate deadline;
    LocalDate closingDate;
    ProjectStatusEnum status;
    Integer iterationDuration;
    Integer configuredIterationCount;
    int representativeCount;
    int stageCount;
    int iterationCount;
    String currentStage;
    Integer currentIteration;
}


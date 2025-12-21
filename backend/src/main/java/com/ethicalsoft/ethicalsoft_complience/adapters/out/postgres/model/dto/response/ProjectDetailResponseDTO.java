package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
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
    TimelineStatusEnum timelineStatus;
    Integer iterationDuration;
    Integer configuredIterationCount;
    int representativeCount;
    int stageCount;
    int iterationCount;
    String currentStage;
    Integer currentIteration;
    String currentSituation;
}

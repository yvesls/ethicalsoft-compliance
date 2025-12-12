package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.TimelineStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSummaryResponseDTO {
	private Long id;
	private String name;
	private String type;
	private LocalDate startDate;
	private LocalDate deadline;
	private ProjectStatusEnum status;
	private TimelineStatusEnum timelineStatus;
	private String currentSituation;
	private int representativeCount;
	private int stageCount;
	private int iterationCount;
	private String currentStage;
	private Integer currentIteration;
}
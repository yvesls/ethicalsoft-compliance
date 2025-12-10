package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ProjectResponseDTO {
	private Long id;
	private String name;
	private String type;
	private LocalDate startDate;
	private int representativeCount;
	private int stageCount;
	private int iterationCount;
}
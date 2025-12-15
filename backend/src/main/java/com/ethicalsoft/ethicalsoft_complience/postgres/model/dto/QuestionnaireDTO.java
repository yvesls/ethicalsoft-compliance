package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class QuestionnaireDTO {
	private String name;
	private String iteration;
	private Integer weight;
	private String stageName;
	private String iterationName;
	private List<String> stageNames;
	private LocalDate applicationStartDate;
	private LocalDate applicationEndDate;
	private Long projectId;
	private Set<QuestionDTO> questions;
	private StageDTO stage;
	private IterationDTO iterationRef;
	private Set<String> iterationStageNames;
}

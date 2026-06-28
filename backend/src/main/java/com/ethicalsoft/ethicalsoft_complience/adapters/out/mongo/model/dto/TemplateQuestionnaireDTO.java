package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TemplateQuestionnaireDTO {
	private String name;
	private BigDecimal weight;
	private String stageName;
	private String iterationRefName;
	private List<String> stageNames;
	private LocalDate applicationStartDate;
	private LocalDate applicationEndDate;
	private String domain;
	private String description;
	private List<TemplateQuestionDTO> questions;
}
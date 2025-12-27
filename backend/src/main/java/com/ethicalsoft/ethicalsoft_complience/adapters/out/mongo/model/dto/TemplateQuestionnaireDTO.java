package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateQuestionnaireDTO {
	private String name;
	private String stageName;
	private String iterationRefName;
	private List<String> stageNames;
	private List<TemplateQuestionDTO> questions;
}
package com.ethicalsoft.ethicalsoft_complience.mongo.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateQuestionnaireDTO {
	private String name;
	private String stageName;
	private String iterationRefName;

	private List<TemplateQuestionDTO> questions;
}
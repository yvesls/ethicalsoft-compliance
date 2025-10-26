package com.ethicalsoft.ethicalsoft_complience.mongo.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class TemplateQuestionDTO {
	private String value;
	private String stageName;
	private Set<String> roleNames;
}
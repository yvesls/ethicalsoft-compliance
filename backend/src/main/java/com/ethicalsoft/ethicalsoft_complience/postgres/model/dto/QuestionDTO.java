package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class QuestionDTO {
	private String value;
	private Set<String> roles;
	private StageDTO stage;
	private String categoryStageName;
}

package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class QuestionDTO {
	private String value;
	private Set<Long> roleIds;
	private StageDTO stage;
	private String categoryStageName;
	private List<String> stageNames;
	private List<StageDTO> stages;
}

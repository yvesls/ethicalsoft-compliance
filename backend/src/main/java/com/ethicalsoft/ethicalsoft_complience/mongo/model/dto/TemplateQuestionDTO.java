package com.ethicalsoft.ethicalsoft_complience.mongo.model.dto;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.RoleSummaryResponseDTO;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class TemplateQuestionDTO {
	private String value;
	private String stageName;
	private Set<RoleSummaryResponseDTO> roles;
	private List<String> stageNames;
}
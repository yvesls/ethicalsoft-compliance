package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RoleSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.StageSummaryResponseDTO;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class TemplateQuestionDTO {
	private String value;
	private String stageName;
	private Set<RoleSummaryResponseDTO> roles;
	private List<StageSummaryResponseDTO> stages;
}
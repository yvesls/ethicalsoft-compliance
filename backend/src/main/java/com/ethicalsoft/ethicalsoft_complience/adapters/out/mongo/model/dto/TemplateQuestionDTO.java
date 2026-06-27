package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RoleSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.StageSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionClassificationEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionTypeEnum;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class TemplateQuestionDTO {
	private String value;
	private QuestionTypeEnum type;
	private QuestionClassificationEnum classification;
	private String stageName;
	private Set<RoleSummaryResponseDTO> roles;
	private List<StageSummaryResponseDTO> stages;
}
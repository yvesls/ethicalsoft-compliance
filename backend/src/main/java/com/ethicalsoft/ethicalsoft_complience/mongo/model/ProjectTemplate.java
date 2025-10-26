package com.ethicalsoft.ethicalsoft_complience.mongo.model;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateIterationDTO;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateQuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateStageDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document( collection = "project_templates" )
public class ProjectTemplate {
	@Id
	private String id;
	private String name;
	private ProjectTypeEnum type;
	private String description;
	private Integer defaultIterationCount;
	private Integer defaultIterationDuration;
	private List<TemplateStageDTO> stages;
	private List<TemplateQuestionnaireDTO> questionnaires;
	private List<TemplateIterationDTO> iterations;
}
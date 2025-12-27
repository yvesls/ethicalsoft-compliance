package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateListDTO {
	private String id;
	private String name;
	private String description;
	private ProjectTypeEnum type;
}
package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.TemplateVisibilityEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTemplateRequestDTO {
	@NotEmpty
	private String name;
	private String description;
	@NotNull
	private TemplateVisibilityEnum visibility;
}
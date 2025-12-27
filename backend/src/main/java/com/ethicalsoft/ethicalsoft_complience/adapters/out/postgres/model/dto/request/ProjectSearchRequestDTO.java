package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request;

import lombok.Data;

@Data
public class ProjectSearchRequestDTO {
	private String name;
	private String code;
	private String type;
	private String status;
}
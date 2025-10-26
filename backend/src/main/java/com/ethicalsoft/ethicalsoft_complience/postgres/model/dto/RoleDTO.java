package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class RoleDTO {
	private String name;
	private String description;
	private Set<RepresentativeDTO> representatives;
}

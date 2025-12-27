package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class RoleDTO {
	private String name;
	private String description;
	private Set<RepresentativeDTO> representatives;
}

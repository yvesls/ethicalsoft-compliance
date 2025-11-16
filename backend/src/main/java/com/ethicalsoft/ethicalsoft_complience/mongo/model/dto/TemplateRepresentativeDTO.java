package com.ethicalsoft.ethicalsoft_complience.mongo.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class TemplateRepresentativeDTO {
	private String email;
	private String firstName;
	private String lastName;
	private BigDecimal weight;
	private Set<String> roleNames;
}
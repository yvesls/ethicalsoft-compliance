package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TemplateIterationDTO {
	private String name;
	private BigDecimal weight;
	private LocalDate applicationStartDate;
	private LocalDate applicationEndDate;
}

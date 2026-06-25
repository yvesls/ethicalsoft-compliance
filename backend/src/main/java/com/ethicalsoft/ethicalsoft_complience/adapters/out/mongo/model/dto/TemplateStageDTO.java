package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TemplateStageDTO {
	private String name;
	private BigDecimal weight;
	private int sequence;
	private Integer durationDays;
	private LocalDate applicationStartDate;
	private LocalDate applicationEndDate;
}
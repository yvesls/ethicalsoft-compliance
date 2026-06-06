package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StageDTO {
	private String name;
	private BigDecimal weight;
	private Integer sequence;
	private Integer durationDays;
	private LocalDate applicationStartDate;
	private LocalDate applicationEndDate;
	private Long projectId;
}
package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StageDTO {
	private String name;
	private BigDecimal weight;
	private Long projectId;
}
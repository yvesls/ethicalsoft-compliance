package com.ethicalsoft.ethicalsoft_complience.mongo.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TemplateStageDTO {
	private String name;
	private BigDecimal weight;
	private int sequence;
}
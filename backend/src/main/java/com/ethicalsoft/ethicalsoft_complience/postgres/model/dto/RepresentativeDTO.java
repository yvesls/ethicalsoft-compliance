package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class RepresentativeDTO {
	private Long userId;
	private Set<Long> roleIds;
	private BigDecimal weight;
	private Long projectId;
}
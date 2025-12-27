package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageResponseDTO {
	private Integer id;
	private String name;
	private BigDecimal weight;

	public static StageResponseDTO fromEntity( Stage stage ) {
		return new StageResponseDTO( stage.getId(), stage.getName(), stage.getWeight() );
	}
}
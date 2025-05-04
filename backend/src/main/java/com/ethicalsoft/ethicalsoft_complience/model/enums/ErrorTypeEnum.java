package com.ethicalsoft.ethicalsoft_complience.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorTypeEnum {

	ERROR( "error" ),
	INFO( "info" );

	private final String label;
}

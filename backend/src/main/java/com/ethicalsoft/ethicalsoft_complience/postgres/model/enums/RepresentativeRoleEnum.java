package com.ethicalsoft.ethicalsoft_complience.postgres.model.enums;

import lombok.Getter;

@Getter
public enum RepresentativeRoleEnum {
	DEVELOPER( "Developer" ),
	CLIENT( "client" ),
	PROJECT_MANAGER( "Project Manager" ),
	QUANTITY_ANALYST( "Quantity Analyst" ),
	SOFTWARE_ARCHITECT( "Software Architect" );

	private final String value;

	RepresentativeRoleEnum( String value ) {
		this.value = value;
	}
}

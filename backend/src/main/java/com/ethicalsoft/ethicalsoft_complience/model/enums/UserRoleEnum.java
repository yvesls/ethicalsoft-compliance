package com.ethicalsoft.ethicalsoft_complience.model.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
	ADMIN( "ROLE_ADMIN" ),
	USER( "USER_ADMIN" );

	private final String value;

	UserRoleEnum( String value ) {
		this.value = value;
	}
}

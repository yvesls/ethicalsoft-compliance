package com.ethicalsoft.ethicalsoft_complience.postgres.model.enums;

import lombok.Getter;

import java.util.stream.Stream;

@Getter
public enum ProjectTypeEnum {
	CASCATA( "Cascata" ), ITERATIVO( "Iterativo Incremental" );

	private final String aString;

	ProjectTypeEnum( String aString ) {
		this.aString = aString;
	}

	public static ProjectTypeEnum fromValue( String value ) {
		return Stream.of( ProjectTypeEnum.values() ).filter( type -> type.aString.equalsIgnoreCase( value ) || type.name().equalsIgnoreCase( value ) ).findFirst().orElseThrow( () -> new IllegalArgumentException( "Tipo de projeto desconhecido: " + value ) );
	}
}

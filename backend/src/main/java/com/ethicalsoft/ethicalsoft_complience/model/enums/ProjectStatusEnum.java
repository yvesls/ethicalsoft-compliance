package com.ethicalsoft.ethicalsoft_complience.model.enums;

import lombok.Getter;

@Getter
public enum ProjectStatusEnum {
	ABERTO( "Aberto" ),
	CONCLUIDO( "Concluído" ),
	RASCUNHO( "Rascunho" ),
	ARQUIVADO( "Arquivado" );

	private final String value;

	ProjectStatusEnum( String value ) {
		this.value = value;
	}
}

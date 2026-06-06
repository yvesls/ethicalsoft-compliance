package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums;

import lombok.Getter;

@Getter
public enum ProjectStatusEnum {
	ABERTO( "Aberto" ),
	CONCLUIDO( "Concluído" ),
	RASCUNHO( "Rascunho" ),
	ARQUIVADO( "Arquivado" ),
	EXCLUIDO( "Excluído" );

	private final String value;

	ProjectStatusEnum( String value ) {
		this.value = value;
	}
}

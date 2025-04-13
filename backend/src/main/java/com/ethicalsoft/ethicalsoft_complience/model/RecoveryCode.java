package com.ethicalsoft.ethicalsoft_complience.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryCode {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	@Column( name = "id" )
	private Long id;

	@Column( name = "email" )
	private String email;

	@Column( name = "code" )
	private String code;

	@Column( name = "expiration" )
	private LocalDateTime expiration;

	public RecoveryCode( String email, String code, LocalDateTime expiration ) {
		this.email = email;
		this.code = code;
		this.expiration = expiration;
	}

}
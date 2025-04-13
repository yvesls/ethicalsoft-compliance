package com.ethicalsoft.ethicalsoft_complience.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table( name = "refresh_tokens" )
public class RefreshToken {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private Long id;
	@Column( nullable = false, unique = true )
	private String token;
	@OneToOne
	@JoinColumn( name = "user_id", nullable = false )
	private User user;
	@Column( nullable = false )
	private Instant expiryDate;

	public RefreshToken( String token, User user, Instant expiryDate ) {
		this.token = token;
		this.user = user;
		this.expiryDate = expiryDate;
	}

}
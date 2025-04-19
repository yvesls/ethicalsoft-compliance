package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenService {

	@Value( value = "${api.security.token.secret}" )
	private String secret;

	public String generateToken( User user ) {
		try {
			var algorithm = Algorithm.HMAC256( secret );
			Instant expiry = Instant.now().plus(30, ChronoUnit.MINUTES);
			return JWT.create()
					.withIssuer( "auth-api" )
					.withExpiresAt( Date.from(expiry))
					.withSubject( user.getUsername() )
					.withClaim(
							"roles",
							user.getAuthorities()
									.stream()
									.map( GrantedAuthority::getAuthority )
									.toList()
					)
					.withClaim( "email", user.getEmail()	)
					.withClaim( "name", user.getFirstName() )
					.withClaim( "isFirstAccess", user.isFirstAccess() )
					.sign( algorithm );

		} catch ( JWTCreationException e ) {
			throw new BusinessException( "Error while generating token", e );
		}
	}

	public String validateToken( String token ) {
		try {
			var algorithm = Algorithm.HMAC256( secret );
			return JWT.require( algorithm ).withIssuer( "auth-api" ).build().verify( token ).getSubject();
		} catch (JWTVerificationException e) {
			throw new BusinessException("Invalid or expired JWT token", e);
		}
	}
	
}

package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@Slf4j
public class TokenService {

	@Value( value = "${api.security.token.secret}" )
	private String secret;

	public String generateToken( User user ) {
		try {
			log.info("[token] Gerando token para usuário id={} email={}", user != null ? user.getId() : null, user != null ? user.getEmail() : null);
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
                    .withClaim( "email", user.getEmail() )
                    .withClaim( "name", user.getFirstName() )
                    .withClaim( "isFirstAccess", user.isFirstAccess() )
                    .withClaim( "authProvider", user.getAuthProvider() != null ? user.getAuthProvider().name() : "LOCAL" )
                    .withClaim( "avatarUrl", user.getAvatarUrl() )
                    .sign( algorithm );

		} catch ( JWTCreationException e ) {
			log.error("[token] Erro ao gerar token para usuário {}", user != null ? user.getEmail() : null, e);
			throw new BusinessException( "Error while generating token", e );
		}
	}

	public String validateToken( String token ) {
		try {
			var algorithm = Algorithm.HMAC256( secret );
			String subject = JWT.require( algorithm ).withIssuer( "auth-api" ).build().verify( token ).getSubject();
			log.debug("[token] Token válido para subject {}", subject);
			return subject;
		} catch (JWTVerificationException e) {
			log.warn("[token] Token inválido ou expirado", e);
			throw new BusinessException("Invalid or expired JWT token", e);
		}
	}
	
}

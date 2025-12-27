package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomAuthenticationProvider implements AuthenticationProvider {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public CustomAuthenticationProvider( UserRepository userRepository, PasswordEncoder passwordEncoder ) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public Authentication authenticate( Authentication authentication ) throws AuthenticationException {
		String username = authentication.getName();
		log.info("[auth-provider] Autenticando usuário {}", username);
		String password = authentication.getCredentials().toString();

		var user = userRepository.findByEmail( username ).orElseThrow( () -> {
			log.warn("[auth-provider] Usuário {} não encontrado", username);
			return new UsernameNotFoundException( "Username not found." );
		} );

		if ( passwordEncoder.matches( password, user.getPassword() ) ) {
			log.info("[auth-provider] Usuário {} autenticado com sucesso", username);
			return new UsernamePasswordAuthenticationToken( user, null, user.getAuthorities() );
		}

		log.warn("[auth-provider] Senha inválida para usuário {}", username);
		throw new BadCredentialsException( "The password provided does not match the username provided." );
	}

	@Override
	public boolean supports( Class<?> authentication ) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom( authentication );
	}
}

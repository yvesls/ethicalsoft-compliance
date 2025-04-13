package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.ethicalsoft.ethicalsoft_complience.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
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
		String password = authentication.getCredentials().toString();

		var user = userRepository.findByEmail( username ).orElseThrow( () -> new UsernameNotFoundException( "User not found" ) );

		if ( passwordEncoder.matches( password, user.getPassword() ) ) {
			return new UsernamePasswordAuthenticationToken( user, null, user.getAuthorities() );
		}

		throw new BadCredentialsException( "Invalid credentials" );
	}

	@Override
	public boolean supports( Class<?> authentication ) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom( authentication );
	}
}

package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.UserRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@AllArgsConstructor
@EnableMethodSecurity
@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

	private final SecurityFilter securityFilter;
	private final CorsConfigurationSource corsConfigurationSource;

	@Bean
	public SecurityFilterChain securityFilterChain( HttpSecurity httpSecurity ) throws Exception {
		return httpSecurity.cors( cors ->
						cors.configurationSource( corsConfigurationSource ) )
				// CSRF desativado de forma intencional e segura: a API é stateless (SessionCreationPolicy.STATELESS)
				// e autentica exclusivamente via header "Authorization: Bearer <JWT>" (ver SecurityFilter).
				// Não há cookies/tokens de sessão enviados automaticamente pelo navegador, portanto a classe
				// de ataques mitigada por CSRF não se aplica a este serviço.
				.csrf( csrf -> csrf.disable() ) // NOSONAR: S4502 - sem estado de sessão nem cookies de autenticação
				.sessionManagement( session ->
						session.sessionCreationPolicy( SessionCreationPolicy.STATELESS ) )
				.authorizeHttpRequests( auth ->
						auth.dispatcherTypeMatchers( DispatcherType.ASYNC, DispatcherType.ERROR ).permitAll()
								.requestMatchers( HttpMethod.OPTIONS, "/**" ).permitAll()
								.requestMatchers( "/auth/**" ).permitAll()
								.anyRequest()
								.authenticated()
				).exceptionHandling( exception ->
						exception.authenticationEntryPoint(
								( request, response, authException ) ->
										response.sendError( HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized" ) ) )
				.addFilterBefore( securityFilter, UsernamePasswordAuthenticationFilter.class )
				.headers(headers -> headers
						.contentSecurityPolicy( csp -> csp.policyDirectives( "default-src 'self'" ) )
				).build();
	}

	@Bean
	public AuthenticationManager authenticationManager( PasswordEncoder passwordEncoder, UserRepository userRepository ) {
		var provider = new CustomAuthenticationProvider( userRepository, passwordEncoder );
		return new ProviderManager( provider );
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
				.csrf( AbstractHttpConfigurer::disable )
				.sessionManagement( session ->
						session.sessionCreationPolicy( SessionCreationPolicy.STATELESS ) )
				.authorizeHttpRequests( auth ->
						auth.requestMatchers( HttpMethod.OPTIONS, "/**" ).permitAll()
								.requestMatchers( HttpMethod.POST, "/auth/**" ).permitAll()
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
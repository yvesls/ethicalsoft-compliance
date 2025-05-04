package com.ethicalsoft.ethicalsoft_complience.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;


@Configuration
public class CorsConfig {

	@Value( "${cors.allowed-origins}" )
	private String allowedOrigins;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		List<String> origins = Arrays.stream( allowedOrigins.split( "," ) )
				.map( String::trim )
				.filter( s -> !s.isEmpty() )
				.toList();
		config.setAllowedOrigins( origins );
		config.setAllowedMethods( Arrays.asList( "GET", "POST", "PUT", "DELETE", "OPTIONS" ) );
		config.setAllowedHeaders( Arrays.asList(
				"Authorization",
				"Content-Type",
				"X-Project-Id",
				"Access-Control-Allow-Origin",
				"Access-Control-Allow-Credentials"
		) );
		config.setAllowedHeaders( Arrays.asList(
				"Authorization",
				"Content-Type",
				"X-Project-Id"
		));
		config.setAllowCredentials( true );
		config.setMaxAge( 3600L );

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration( "/**", config );
		return source;
	}
}

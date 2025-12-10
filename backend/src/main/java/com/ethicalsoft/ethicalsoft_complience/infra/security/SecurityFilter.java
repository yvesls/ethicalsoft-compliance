package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class SecurityFilter extends OncePerRequestFilter {

	private final TokenService tokenService;
	private final UserRepository userRepository;
	private final RepresentativeRepository representativeRepository;

	@Override
	protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException {
		try {
			var token = extractToken( request );
			if ( Objects.nonNull( token ) ) {
				var username = tokenService.validateToken( token );
				userRepository.findByEmail( username ).ifPresent( user -> authenticateUser( request, user ) );
			}
		} catch ( Exception ex ) {
			logger.warn( "JWT inválido ou expirado", ex );
			response.sendError( HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized" );
			return;
		}
		filterChain.doFilter( request, response );
	}

	private void authenticateUser( HttpServletRequest request, User user ) {
		var projectId = extractProjectId( request );
		var authorities = getAuthorities( user, projectId );

		var authentication = new UsernamePasswordAuthenticationToken( user, null, authorities );
		SecurityContextHolder.getContext().setAuthentication( authentication );
	}

	private List<SimpleGrantedAuthority> getAuthorities( User user, Long projectId ) {
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		authorities.add( new SimpleGrantedAuthority( UserRoleEnum.USER.name() ) );

		if ( UserRoleEnum.ADMIN.equals( user.getRole() ) ) {
			authorities.add( new SimpleGrantedAuthority( UserRoleEnum.ADMIN.name() ) );
		}

		if ( Objects.nonNull( projectId ) ) {
			representativeRepository.findByUserEmailAndProjectId( user.getEmail(), projectId )
					.ifPresentOrElse( rep -> authorities.addAll( rep.getRoles().stream()
									.map( role -> new SimpleGrantedAuthority( "ROLE_" + role.getName().toUpperCase() ) )
									.toList()
							),
							() -> logger.debug("Usuário {} não é representante no projeto {} ou não há roles associadas")
					);
		}

		return authorities;
	}

	private String extractToken( HttpServletRequest request ) {
		var authHeader = request.getHeader( "Authorization" );
		return ( Objects.nonNull( authHeader ) && authHeader.startsWith( "Bearer " ) ) ? authHeader.substring( "Bearer ".length() ) : null;
	}

	private Long extractProjectId( HttpServletRequest request ) {
		String projectIdHeader = request.getHeader( "X-Project-Id" );
		return ObjectUtils.isEmpty( projectIdHeader ) ? null : Long.valueOf( projectIdHeader );
	}
}

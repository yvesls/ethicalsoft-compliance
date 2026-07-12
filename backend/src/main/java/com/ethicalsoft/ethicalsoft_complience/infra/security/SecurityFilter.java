package com.ethicalsoft.ethicalsoft_complience.infra.security;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
@Slf4j
public class SecurityFilter extends OncePerRequestFilter {

	private final TokenService tokenService;
	private final UserRepository userRepository;
	private final RepresentativeRepository representativeRepository;
	private final ProjectRepository projectRepository;

	@Override
	protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException {
		var token = extractToken( request );
		if ( Objects.nonNull( token ) ) {
			try {
				var username = tokenService.validateToken( token );
				userRepository.findWithRepresentativesByEmail( username ).ifPresentOrElse(
					user -> authenticateUser( request, user ),
					() -> log.warn("[security-filter] Usuário {} não encontrado ao validar token", username)
				);
			} catch ( Exception ex ) {
				log.warn( "[security-filter] JWT inválido ou expirado: {}", ex.getMessage() );
			}
		}
		filterChain.doFilter( request, response );
	}

	private void authenticateUser( HttpServletRequest request, User user ) {
		var projectId = extractProjectId( request );

		List<SimpleGrantedAuthority> authorities;
		try {
			authorities = getAuthorities( user, projectId );
		} catch ( RuntimeException ex ) {
			// A resolução de papéis do projeto nunca deve impedir a autenticação do usuário.
			log.warn( "[security-filter] Falha ao resolver authorities do projeto {} para {}: {}. Autenticando com papel base.",
					projectId, user.getEmail(), ex.getMessage() );
			authorities = new ArrayList<>( List.of( new SimpleGrantedAuthority( UserRoleEnum.USER.name() ) ) );
		}

		var authentication = new UsernamePasswordAuthenticationToken( user, null, authorities );
		SecurityContextHolder.getContext().setAuthentication( authentication );
		log.debug("[security-filter] Usuário {} autenticado com roles {}", user.getEmail(), authorities);
	}

	private List<SimpleGrantedAuthority> getAuthorities( User user, Long projectId ) {
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		authorities.add( new SimpleGrantedAuthority( UserRoleEnum.USER.name() ) );

		if ( UserRoleEnum.ADMIN.equals( user.getRole() ) ) {
			authorities.add( new SimpleGrantedAuthority( UserRoleEnum.ADMIN.name() ) );
			return authorities;
		}

		if ( Objects.nonNull( projectId ) ) {
			if ( projectRepository.existsByIdAndOwnerId(projectId, user.getId()) ) {
				authorities.add(new SimpleGrantedAuthority(UserRoleEnum.ADMIN.name()));
				return authorities;
			}

			representativeRepository.findByUserIdAndProjectId( user.getId(), projectId )
				.ifPresent(rep -> appendRepresentativeAuthorities(authorities, rep));
		}

		return authorities;
	}

	private void appendRepresentativeAuthorities(List<SimpleGrantedAuthority> authorities, Representative representative) {
		representative.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizeRoleName(role.getName()))));
	}

	private String normalizeRoleName(String rawName) {
		if (rawName == null) {
			return "";
		}
		return rawName.trim().toUpperCase().replaceAll("\\s+", "_");
	}

	private String extractToken( HttpServletRequest request ) {
		var authHeader = request.getHeader( "Authorization" );
		return ( Objects.nonNull( authHeader ) && authHeader.startsWith( "Bearer " ) ) ? authHeader.substring( "Bearer ".length() ) : null;
	}

	private Long extractProjectId( HttpServletRequest request ) {
		String projectIdHeader = request.getHeader( "X-Project-Id" );
		if ( projectIdHeader == null ) {
			return null;
		}
		String value = projectIdHeader.trim();
		// Ignora valores ausentes ou não numéricos (ex.: "null"/"undefined" enviados pelo frontend).
		// Um header malformado jamais deve quebrar a autenticação.
		if ( value.isEmpty() || !value.matches( "\\d+" ) ) {
			log.debug( "[security-filter] Header X-Project-Id ignorado (valor inválido): '{}'", value );
			return null;
		}
		try {
			return Long.valueOf( value );
		} catch ( NumberFormatException ex ) {
			return null;
		}
	}
}

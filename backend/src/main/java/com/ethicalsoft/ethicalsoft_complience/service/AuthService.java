package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.UserNotFoundException;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.LoginDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.RegisterUserDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ErrorTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService implements UserDetailsService {

	private final UserRepository userRepository;

	private final ModelMapper modelMapper = new ModelMapper();

	private final AuthenticationManager authenticationManager;

	public Authentication token( LoginDTO loginDTO ) {
		try {
			String username = loginDTO != null ? loginDTO.getUsername() : null;
			log.info("[auth] Iniciando autenticação do usuário {}", username);
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken( username, loginDTO != null ? loginDTO.getPassword() : null );
			Authentication authenticated = authenticationManager.authenticate( token );
			log.info("[auth] Autenticação bem-sucedida para usuário {}", username);
			return authenticated;
		} catch ( Exception ex ) {
			log.error("[auth] Falha ao autenticar usuário {}", loginDTO != null ? loginDTO.getUsername() : null, ex);
			throw ex;
		}
	}

	public void register( RegisterUserDTO registerUserDTO ) {
		try {
			log.info("[auth] Registrando novo usuário email={}", registerUserDTO != null ? registerUserDTO.getEmail() : null);
			if ( registerUserDTO == null || !registerUserDTO.isAcceptedTerms() ) {
				throw new BusinessException( ErrorTypeEnum.INFO, "The terms were not accepted" );
			}

			var dataUser = userRepository.findByEmail( registerUserDTO.getEmail() );

			if ( dataUser.isPresent() ) {
				throw new BusinessException( ErrorTypeEnum.INFO, "Email already exists" );
			}
			var encryptedPassword = new BCryptPasswordEncoder().encode( registerUserDTO.getPassword() );
			registerUserDTO.setPassword( encryptedPassword );

			var newUser = modelMapper.map( registerUserDTO, User.class );
			newUser.setRole( UserRoleEnum.ADMIN );
			newUser.setFirstAccess( false );

			this.userRepository.save( newUser );
			log.info("[auth] Usuário {} criado com sucesso", registerUserDTO.getEmail());
		} catch ( Exception ex ) {
			log.error("[auth] Falha ao registrar usuário {}", registerUserDTO != null ? registerUserDTO.getEmail() : null, ex);
			throw ex;
		}
	}

	public User getAuthenticatedUser() {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication == null || !authentication.isAuthenticated()) {
				log.warn("[auth] Tentativa de acesso sem usuário autenticado");
				throw new SecurityException("Nenhum usuário autenticado encontrado.");
			}
			User user = (User) authentication.getPrincipal();
			log.debug("[auth] Contexto carregado para usuário id={}", user.getId());
			return user;
		} catch ( Exception ex ) {
			log.error("[auth] Falha ao obter usuário autenticado", ex);
			throw ex;
		}
	}

	public Long getAuthenticatedUserId() {
		return getAuthenticatedUser().getId();
	}

    @Override
    public UserDetails loadUserByUsername(String username) {
        return this.userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Deprecated
    public User loadUserByEmail(String email) {
        return this.userRepository.findByEmail( email )
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}

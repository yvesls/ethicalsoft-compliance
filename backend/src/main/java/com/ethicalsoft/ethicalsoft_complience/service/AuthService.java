package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.model.dto.auth.LoginDTO;
import com.ethicalsoft.ethicalsoft_complience.model.dto.auth.RegisterUserDTO;
import com.ethicalsoft.ethicalsoft_complience.model.enums.ErrorTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

	private final UserRepository userRepository;

	private final ModelMapper modelMapper = new ModelMapper();

	private final AuthenticationManager authenticationManager;

	public Authentication token( LoginDTO loginDTO ) {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken( loginDTO.getUsername(), loginDTO.getPassword() );
		return authenticationManager.authenticate( token );
	}

	public void register( RegisterUserDTO registerUserDTO ) {
		if ( !registerUserDTO.isAcceptedTerms() ) {
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
	}

}

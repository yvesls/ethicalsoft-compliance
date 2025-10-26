package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.UserDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ErrorTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class UserService {

	private final UserRepository userRepository;
	private final ModelMapper modelMapper = new ModelMapper();

	public Page<UserDTO> findAll( Pageable pageable ) {
		return userRepository.findAll( pageable ).map( user -> modelMapper.map( user, UserDTO.class ) );
	}

	public UserDTO findById( Long id ) {
		return userRepository.findById( id ).map( user -> modelMapper.map( user, UserDTO.class ) ).orElseThrow( () -> new BusinessException( ErrorTypeEnum.INFO, "User not found with id: " + id ) );
	}

}

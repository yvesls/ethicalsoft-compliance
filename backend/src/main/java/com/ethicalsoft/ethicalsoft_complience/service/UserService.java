package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.UserDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ErrorTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

	private final UserRepository userRepository;
	private final ModelMapper modelMapper = new ModelMapper();

	public Page<UserDTO> findAll( Pageable pageable ) {
		try {
			log.info("[user] Listando usuários página={} tamanho={}", pageable != null ? pageable.getPageNumber() : null, pageable != null ? pageable.getPageSize() : null);
			Page<UserDTO> page = userRepository.findAll( pageable ).map( user -> modelMapper.map( user, UserDTO.class ) );
			log.info("[user] Página retornou {} registros", page.getNumberOfElements());
			return page;
		} catch ( Exception ex ) {
			log.error("[user] Falha ao listar usuários", ex);
			throw ex;
		}
	}

	public UserDTO findById( Long id ) {
		try {
			log.info("[user] Buscando usuário id={}", id);
			return userRepository.findById( id )
					.map( user -> modelMapper.map( user, UserDTO.class ) )
					.orElseThrow( () -> new BusinessException( ErrorTypeEnum.INFO, "User not found with id: " + id ) );
		} catch ( Exception ex ) {
			log.error("[user] Falha ao buscar usuário id={}", id, ex);
			throw ex;
		}
	}

}

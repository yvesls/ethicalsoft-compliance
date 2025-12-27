package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserQueryPort {
    Page<UserDTO> findAll(Pageable pageable);
    UserDTO findById(Long id);
}


package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.user.GetUserByIdUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.user.ListUsersUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.base.BaseController;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping( "user" )
public class UserController extends BaseController {

    private final ListUsersUseCase listUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;

    @GetMapping( "/" )
    @PreAuthorize( "hasRole('ADMIN')" )
    public Page< UserDTO > findAll( @PageableDefault( sort = "id", direction = Sort.Direction.ASC ) Pageable pageable ) {
        return listUsersUseCase.execute( pageable );
    }

    @GetMapping( "/{id}" )
    @PreAuthorize( "hasRole('ADMIN')" )
    public UserDTO findById( @PathVariable Long id ) {
        return getUserByIdUseCase.execute( id );
    }
}

package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.controller.base.BaseController;
import com.ethicalsoft.ethicalsoft_complience.model.dto.UserDTO;
import com.ethicalsoft.ethicalsoft_complience.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
	private final UserService userService;

	@GetMapping( "/" )
	@PreAuthorize( "hasRole('ADMIN')" )
	public ResponseEntity< Page< UserDTO > > findAll( @PageableDefault( sort = "id", direction = Sort.Direction.ASC ) Pageable pageable ) {
		return ResponseEntity.ok( this.userService.findAll( pageable ) );
	}

	@GetMapping( "/{id}" )
	@PreAuthorize( "hasRole('ADMIN')" )
	public ResponseEntity< UserDTO > findById( @PathVariable Long id ) {
		return ResponseEntity.ok( this.userService.findById( id ) );
	}
}

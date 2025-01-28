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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin
@RequiredArgsConstructor
@RestController
@RequestMapping( "user" )
public class UserController extends BaseController {
    private final UserService userService;

    @GetMapping( "/" )
    public ResponseEntity<Page<UserDTO>> findAll(@PageableDefault( sort = "id", direction = Sort.Direction.ASC ) Pageable pageable ) {
        return ResponseEntity.ok( this.userService.findAll( pageable ) );
    }
}

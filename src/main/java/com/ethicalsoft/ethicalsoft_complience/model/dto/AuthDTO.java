package com.ethicalsoft.ethicalsoft_complience.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import util.validators.annotations.RequiredField;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthDTO {

    @RequiredField( label = "username" )
    @Email
    private String username;

    @RequiredField( label = "password" )
    private String password;

}

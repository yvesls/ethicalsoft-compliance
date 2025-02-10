package com.ethicalsoft.ethicalsoft_complience.model.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import util.validators.annotations.RequiredField;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long id;

    @RequiredField( label = "First name" )
    private String firstName;

    @RequiredField( label = "Last name" )
    private String lastName;

    @Email
    private String email;

    private boolean acceptedTerms;

}

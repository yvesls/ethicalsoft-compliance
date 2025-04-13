package com.ethicalsoft.ethicalsoft_complience.model.dto;

import com.ethicalsoft.ethicalsoft_complience.model.enums.UserRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import util.validators.annotations.Email;
import util.validators.annotations.RequiredField;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserDTO {

	@RequiredField( label = "First name" )
	private String firstName;

	@RequiredField( label = "Last name" )
	private String lastName;

	@Email
	private String email;

	@RequiredField( label = "Password" )
	private String password;

	private boolean acceptedTerms;

	private boolean firstAccess;

	@RequiredField( label = "role" )
	private UserRoleEnum role;
}

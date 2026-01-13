package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.common.validation.validators.annotations.RequiredField;
import com.ethicalsoft.ethicalsoft_complience.common.validation.validators.annotations.ValidEmail;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserDTO {

	@RequiredField( label = "First name" )
	private String firstName;

	@RequiredField( label = "Last name" )
	private String lastName;

	@ValidEmail
	private String email;

	@RequiredField( label = "Password" )
	private String password;

	private boolean acceptedTerms;

	private boolean firstAccess;

	@RequiredField( label = "role" )
	private UserRoleEnum role;
}

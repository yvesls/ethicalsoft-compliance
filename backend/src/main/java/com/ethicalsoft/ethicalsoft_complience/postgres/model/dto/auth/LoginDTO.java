package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth;

import com.ethicalsoft.ethicalsoft_complience.util.validators.annotations.RequiredField;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {

	@RequiredField( label = "username" )
	@Email
	private String username;

	@RequiredField( label = "password" )
	private String password;

}

package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth;

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
public class PasswordResetDTO {

	@ValidEmail
	private String email;

	@RequiredField( label = "newPassword" )
	private String newPassword;

	private boolean firstAccessFlow = false;
}
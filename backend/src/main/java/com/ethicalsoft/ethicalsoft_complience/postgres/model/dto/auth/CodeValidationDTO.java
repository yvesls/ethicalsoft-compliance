package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth;

import com.ethicalsoft.ethicalsoft_complience.util.validators.annotations.Email;
import com.ethicalsoft.ethicalsoft_complience.util.validators.annotations.RequiredField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CodeValidationDTO {

	@Email
	private String email;

	@RequiredField( label = "code" )
	private String code;
}
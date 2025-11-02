package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto;

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

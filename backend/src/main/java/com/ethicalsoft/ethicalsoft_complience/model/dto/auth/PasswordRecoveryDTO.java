package com.ethicalsoft.ethicalsoft_complience.model.dto.auth;


import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordRecoveryDTO {

	@Email
	private String email;
}

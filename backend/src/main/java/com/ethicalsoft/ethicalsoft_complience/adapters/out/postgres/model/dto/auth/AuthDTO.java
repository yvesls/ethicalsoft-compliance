package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthDTO {

	private String accessToken;

	private String refreshToken;

}
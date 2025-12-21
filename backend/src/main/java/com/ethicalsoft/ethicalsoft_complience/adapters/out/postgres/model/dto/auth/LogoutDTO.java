package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutDTO {
	private String refreshToken;
}
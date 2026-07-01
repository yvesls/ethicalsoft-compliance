package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExtendSessionResponseDTO {
    private Long newExpirationTime;
    private String accessToken;
}

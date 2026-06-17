package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GoogleAuthDTO {

    @NotBlank(message = "Google ID token is required")
    private String idToken;
}

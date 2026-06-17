package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AcceptTermsDTO {

    @NotBlank
    @Email
    private String email;
}

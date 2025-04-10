package com.ethicalsoft.ethicalsoft_complience.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import util.validators.annotations.Email;
import util.validators.annotations.RequiredField;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetDTO {

    @Email
    private String email;

    @RequiredField(label = "newPassword")
    private String newPassword;
}
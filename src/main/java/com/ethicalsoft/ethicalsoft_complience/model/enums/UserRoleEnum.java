package com.ethicalsoft.ethicalsoft_complience.model.enums;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    ADMIN("Administrator"),
    USER("User");

    UserRoleEnum(String value) {
        this.value = value;
    }

    private String value;
}

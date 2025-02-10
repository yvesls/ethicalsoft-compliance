package com.ethicalsoft.ethicalsoft_complience.model.enums;

import lombok.Getter;

@Getter
public enum RepresentativeRoleEnum {
    DEVELOPER("Developer"),
    CLIENT("client"),
    PROJECT_MANAGER("Project Manager"),
    QUANTITY_ANALYST("Quantity Analyst"),
    SOFTWARE_ARCHITECT("Software Architect");

    RepresentativeRoleEnum(String value) {
        this.value = value;
    }

    private String value;
}

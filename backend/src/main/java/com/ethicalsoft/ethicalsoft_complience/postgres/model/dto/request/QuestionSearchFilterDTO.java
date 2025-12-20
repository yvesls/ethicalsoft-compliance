package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request;

import lombok.Data;

@Data
public class QuestionSearchFilterDTO {
    private String questionText;
    private String roleName;
}


package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request;

import lombok.Data;

@Data
public class QuestionSearchFilterDTO {
    private String questionText;
    private String roleName;
}


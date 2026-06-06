package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RescheduleQuestionnaireRequestDTO {

    @NotNull(message = "A nova data de início da aplicação é obrigatória.")
    private LocalDate newApplicationStartDate;

    @NotNull(message = "A nova data de término da aplicação é obrigatória.")
    private LocalDate newApplicationEndDate;
}


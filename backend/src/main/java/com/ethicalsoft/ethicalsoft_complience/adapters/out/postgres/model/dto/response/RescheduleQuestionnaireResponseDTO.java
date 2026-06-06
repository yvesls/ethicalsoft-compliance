package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RescheduleQuestionnaireResponseDTO {

    private Integer questionnaireId;
    private String questionnaireName;
    private LocalDate oldStartDate;
    private LocalDate oldEndDate;
    private LocalDate newStartDate;
    private LocalDate newEndDate;
    private String newStatus;
    private String stageOrIterationName;
    private LocalDate stageOrIterationNewStart;
    private LocalDate stageOrIterationNewEnd;
    private boolean projectDeadlineExceeded;
    private String projectDeadlineWarning;
    private List<String> notificationsSent;
}


package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class QuestionnaireResponseSummaryDTO {
    Long representativeId;
    QuestionnaireResponseStatus status;
    LocalDateTime submissionDate;
}


package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
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


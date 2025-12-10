package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class RespondentStatusDTO {
    Long representativeId;
    String name;
    String email;
    QuestionnaireResponseStatus status;
    LocalDateTime completedAt;
}


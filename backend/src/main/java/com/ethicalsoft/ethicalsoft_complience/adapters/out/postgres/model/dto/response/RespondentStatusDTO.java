package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RespondentStatusDTO(Long representativeId, String name, String email, QuestionnaireResponseStatus status,
                                  LocalDateTime completedAt) {
}


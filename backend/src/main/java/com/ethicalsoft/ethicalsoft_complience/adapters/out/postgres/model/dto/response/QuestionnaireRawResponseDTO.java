package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record QuestionnaireRawResponseDTO(Integer id, String name, String iteration, BigDecimal weight,
                                          LocalDate applicationStartDate, LocalDate applicationEndDate, Long projectId,
                                          Integer stageId, Integer iterationId, TimelineStatusEnum status) {
}


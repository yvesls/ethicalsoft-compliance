package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.TimelineStatusEnum;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class QuestionnaireRawResponseDTO {
    Integer id;
    String name;
    String iteration;
    Integer weight;
    LocalDate applicationStartDate;
    LocalDate applicationEndDate;
    Long projectId;
    Integer stageId;
    Integer iterationId;
    TimelineStatusEnum status;
}


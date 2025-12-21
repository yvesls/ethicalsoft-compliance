package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class QuestionnaireSummaryResponseDTO {
    Integer id;
    String name;
    LocalDate applicationStartDate;
    LocalDate applicationEndDate;
    String stageName;
    String iterationName;
    Integer totalRespondents;
    Integer respondedRespondents;
    Integer pendingRespondents;
    LocalDateTime lastResponseAt;
    QuestionnaireResponseStatus progressStatus;
    TimelineStatusEnum status;
    List<RespondentStatusDTO> respondents;
}

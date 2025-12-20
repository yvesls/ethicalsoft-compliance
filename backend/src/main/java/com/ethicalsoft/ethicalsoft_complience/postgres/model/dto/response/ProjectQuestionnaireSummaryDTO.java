package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ProjectQuestionnaireSummaryDTO {
    Long projectId;
    Integer questionnaireId;
    String questionnaireName;
    String stageName;
    String iterationName;
    LocalDate applicationStartDate;
    LocalDate applicationEndDate;
    QuestionnaireResponseStatus overallStatus;
    Integer totalRespondents;
    Integer responded;
    Integer pending;
    LocalDateTime lastResponseAt;
    List<RespondentStatusDTO> respondents;
}


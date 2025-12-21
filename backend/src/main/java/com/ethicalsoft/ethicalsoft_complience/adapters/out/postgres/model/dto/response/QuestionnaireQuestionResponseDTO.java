package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QuestionnaireQuestionResponseDTO {
    Long id;
    String text;
    List<Integer> stageIds;
    List<String> stageNames;
    List<Long> roleIds;
    Integer order;
}


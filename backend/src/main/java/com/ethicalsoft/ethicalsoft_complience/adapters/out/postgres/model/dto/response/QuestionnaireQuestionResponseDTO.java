package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record QuestionnaireQuestionResponseDTO(Long id, String text, List<Integer> stageIds, List<String> stageNames,
                                               List<Long> roleIds, List<String> roleNames, Integer order) {
}


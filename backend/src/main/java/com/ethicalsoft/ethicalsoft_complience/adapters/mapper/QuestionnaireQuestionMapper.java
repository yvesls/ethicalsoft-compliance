package com.ethicalsoft.ethicalsoft_complience.adapters.mapper;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Question;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class QuestionnaireQuestionMapper {

    public QuestionnaireQuestionResponseDTO toDto(Question question) {
        if (question == null) {
            return null;
        }

        List<Integer> stageIds = Optional.ofNullable(question.getStages())
                .map(stages -> stages.stream().map(Stage::getId).toList())
                .orElseGet(List::of);

        List<String> stageNames = Optional.ofNullable(question.getStages())
                .map(stages -> stages.stream()
                        .sorted(Comparator.comparing(Stage::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                        .map(Stage::getName)
                        .toList())
                .orElseGet(List::of);

        List<Long> roleIds = Optional.ofNullable(question.getRoles())
                .map(roles -> roles.stream().map(Role::getId).toList())
                .orElseGet(List::of);

        return QuestionnaireQuestionResponseDTO.builder()
                .id(question.getId() != null ? question.getId().longValue() : null)
                .text(question.getValue())
                .stageIds(stageIds)
                .stageNames(stageNames)
                .roleIds(roleIds)
                .order(question.getId())
                .build();
    }
}


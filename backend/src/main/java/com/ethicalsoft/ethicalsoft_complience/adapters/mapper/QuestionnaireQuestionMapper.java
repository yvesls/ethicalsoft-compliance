package com.ethicalsoft.ethicalsoft_complience.adapters.mapper;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Question;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.QuestionnaireQuestionResponseDTO;
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
                .map(stages -> stages.stream().map(stage -> stage.getId()).toList())
                .orElseGet(List::of);

        List<String> stageNames = Optional.ofNullable(question.getStages())
                .map(stages -> stages.stream()
                        .sorted(Comparator.comparing(stage -> stage.getName(), Comparator.nullsLast(String::compareToIgnoreCase)))
                        .map(stage -> stage.getName())
                        .toList())
                .orElseGet(List::of);

        List<Long> roleIds = Optional.ofNullable(question.getRoles())
                .map(roles -> roles.stream().map(role -> role.getId()).toList())
                .orElseGet(List::of);

        List<String> roleNames = Optional.ofNullable(question.getRoles())
                .map(roles -> roles.stream()
                        .sorted(Comparator.comparing(role -> role.getName(), Comparator.nullsLast(String::compareToIgnoreCase)))
                        .map(role -> role.getName())
                        .toList())
                .orElseGet(List::of);

        return QuestionnaireQuestionResponseDTO.builder()
                .id(question.getId() != null ? question.getId().longValue() : null)
                .text(question.getValue())
                .stageIds(stageIds)
                .stageNames(stageNames)
                .roleIds(roleIds)
                .roleNames(roleNames)
                .order(question.getId())
                .build();
    }
}


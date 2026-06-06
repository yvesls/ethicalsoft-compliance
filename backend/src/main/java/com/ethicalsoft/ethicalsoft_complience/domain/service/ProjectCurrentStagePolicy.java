package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

@Service
public class ProjectCurrentStagePolicy {


    public String findCurrentStageName(Set<Stage> stages, LocalDate now) {
        if (ObjectUtils.isNullOrEmpty(stages)) {
            return null;
        }
        return stages.stream()
                .filter(s -> s.getApplicationStartDate() != null && s.getApplicationEndDate() != null &&
                        !now.isBefore(s.getApplicationStartDate()) && !now.isAfter(s.getApplicationEndDate()))
                .sorted(Comparator.comparing(Stage::getSequence))
                .map(Stage::getName)
                .findFirst()
                .orElse(findClosestStageName(stages, now));
    }

    private String findClosestStageName(Set<Stage> stages, LocalDate now) {
        return stages.stream()
                .filter(s -> s.getApplicationStartDate() != null && !now.isBefore(s.getApplicationStartDate()))
                .sorted(Comparator.comparing(Stage::getSequence, Comparator.reverseOrder()))
                .map(Stage::getName)
                .findFirst()
                .orElseGet(() -> stages.stream()
                        .filter(s -> s.getApplicationStartDate() != null)
                        .sorted(Comparator.comparing(Stage::getApplicationStartDate))
                        .map(Stage::getName)
                        .findFirst()
                        .orElse(null));
    }

    public LocalDate findNextQuestionnaireDate(Project project) {
        if (project.getQuestionnaires() == null) {
            return project.getStartDate();
        }
        return project.getQuestionnaires().stream()
                .map(Questionnaire::getApplicationStartDate)
                .filter(Objects::nonNull)
                .filter(date -> !date.isBefore(LocalDate.now()))
                .sorted()
                .findFirst()
                .orElse(project.getStartDate());
    }
}

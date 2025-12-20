package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.util.ObjectUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

@Service
public class ProjectCurrentStagePolicy {

    public String findCurrentStageName(Set<Questionnaire> questionnaires, LocalDate now) {
        if (ObjectUtil.isNullOrEmpty( questionnaires )) {
            return null;
        }
        return questionnaires.stream()
                .filter(q -> q.getApplicationStartDate() != null && q.getApplicationEndDate() != null &&
                        !now.isBefore(q.getApplicationStartDate()) && !now.isAfter(q.getApplicationEndDate()))
                .map(Questionnaire::getStage)
                .filter(Objects::nonNull)
                .map(Stage::getName)
                .findFirst()
                .orElse(null);
    }

    public LocalDate findNextQuestionnaireDate(Project project) {
        return project.getQuestionnaires().stream()
                .map(Questionnaire::getApplicationStartDate)
                .filter(Objects::nonNull)
                .filter(date -> !date.isBefore(LocalDate.now()))
                .sorted()
                .findFirst()
                .orElse(project.getStartDate());
    }
}


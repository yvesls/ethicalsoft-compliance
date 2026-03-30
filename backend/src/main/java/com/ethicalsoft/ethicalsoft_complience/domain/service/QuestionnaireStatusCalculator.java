package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class QuestionnaireStatusCalculator {

    public QuestionnaireResponseStatus calculateStatus(List<QuestionnaireResponse.AnswerDocument> answers) {
        return calculateStatus(answers, false, null);
    }

    public QuestionnaireResponseStatus calculateStatus(List<QuestionnaireResponse.AnswerDocument> answers, boolean draft) {
        return calculateStatus(answers, draft, null);
    }

    public QuestionnaireResponseStatus calculateStatus(List<QuestionnaireResponse.AnswerDocument> answers,
                                                        boolean draft,
                                                        Set<Long> representativeRoleIds) {
        List<QuestionnaireResponse.AnswerDocument> relevant;
        if (representativeRoleIds != null && !representativeRoleIds.isEmpty()) {
            relevant = answers.stream()
                    .filter(ans -> ans.getRoleIds() != null
                            && ans.getRoleIds().stream().anyMatch(representativeRoleIds::contains))
                    .toList();
        } else {
            relevant = answers;
        }

        if (relevant.isEmpty()) {
            return QuestionnaireResponseStatus.PENDING;
        }

        boolean hasAny = relevant.stream().anyMatch(ans -> ans.getResponse() != null);

        if (!hasAny) {
            return QuestionnaireResponseStatus.PENDING;
        }

        if (draft) {
            return QuestionnaireResponseStatus.IN_PROGRESS;
        }

        boolean allAnswered = relevant.stream().allMatch(ans -> ans.getResponse() != null);
        return allAnswered ? QuestionnaireResponseStatus.COMPLETED : QuestionnaireResponseStatus.IN_PROGRESS;
    }
}

package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionnaireStatusCalculator {

    public QuestionnaireResponseStatus calculateStatus(List<QuestionnaireResponse.AnswerDocument> answers) {
        boolean hasAny = answers.stream().anyMatch(ans -> ans.getResponse() != null);
        boolean allAnswered = hasAny && answers.stream().allMatch(ans -> ans.getResponse() != null);

        if (!hasAny) {
            return QuestionnaireResponseStatus.PENDING;
        }
        return allAnswered ? QuestionnaireResponseStatus.COMPLETED : QuestionnaireResponseStatus.IN_PROGRESS;
    }
}


package com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepCalculationResult;

public interface IsepResultCommandPort {
    QuestionnaireResult save(IsepCalculationResult result);
}


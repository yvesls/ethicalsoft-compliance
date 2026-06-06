package com.ethicalsoft.ethicalsoft_complience.domain.isep;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;

import java.util.List;

public interface IsepCalculationStrategy {

    ProjectTypeEnum supportedType();

    IsepCalculationResult calculate(Project project,
                                    Questionnaire questionnaire,
                                    List<QuestionnaireResponse> responses,
                                    List<Representative> representatives);
}


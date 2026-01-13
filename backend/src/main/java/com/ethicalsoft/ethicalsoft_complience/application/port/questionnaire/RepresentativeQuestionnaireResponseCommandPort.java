package com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;

public interface RepresentativeQuestionnaireResponseCommandPort {
    void createResponsesForRepresentative(Project project, Representative representative);
}


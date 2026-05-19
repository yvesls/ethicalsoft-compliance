package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model;

import java.util.Collections;
import java.util.Set;

public record QuestionAnalysis(Set<QuestionDataType> dataTypes, String domainFilter) {

    public static QuestionAnalysis minimal() {
        return new QuestionAnalysis(Collections.emptySet(), null);
    }

    public boolean includes(QuestionDataType type) {
        return dataTypes.contains(type);
    }
}

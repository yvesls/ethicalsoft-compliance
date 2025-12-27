package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireResponseRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.QuestionnaireResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuestionnaireResponseRepositoryAdapter implements QuestionnaireResponseRepositoryPort {

    private final QuestionnaireResponseRepository delegate;

    @Override
    public Optional<QuestionnaireResponse> findByQuestionnaireIdAndRepresentativeId(Integer questionnaireId, Long representativeId) {
        return delegate.findByQuestionnaireIdAndRepresentativeId(questionnaireId, representativeId);
    }

    @Override
    public List<QuestionnaireResponse> findByQuestionnaireId(Integer questionnaireId) {
        return delegate.findByQuestionnaireId(questionnaireId);
    }

    @Override
    public List<QuestionnaireResponse> findByProjectIdAndQuestionnaireId(Long projectId, Integer questionnaireId) {
        return delegate.findByProjectIdAndQuestionnaireId(projectId, questionnaireId);
    }

    @Override
    public List<QuestionnaireResponse> findPendingResponses(Long projectId, Integer questionnaireId) {
        return delegate.findPendingResponses(projectId, questionnaireId);
    }

    @Override
    public Optional<QuestionnaireResponse> findByProjectIdAndQuestionnaireIdAndRepresentativeId(Long projectId, Integer questionnaireId, Long representativeId) {
        return delegate.findByProjectIdAndQuestionnaireIdAndRepresentativeId(projectId, questionnaireId, representativeId);
    }

    @Override
    public List<QuestionnaireResponse> findSummariesByProjectAndQuestionnaire(Long projectId, Integer questionnaireId) {
        return delegate.findSummariesByProjectAndQuestionnaire(projectId, questionnaireId);
    }

    @Override
    public QuestionnaireResponse save(QuestionnaireResponse response) {
        return delegate.save(response);
    }
}


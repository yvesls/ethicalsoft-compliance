package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres;

import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionnaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuestionnaireRepositoryAdapter implements QuestionnaireRepositoryPort {

    private final QuestionnaireRepository delegate;

    @Override
    public List<Questionnaire> findAllByProjectIdWithQuestions(Long projectId) {
        return delegate.findAllByProjectIdWithQuestions(projectId);
    }

    @Override
    public Page<Questionnaire> findByProjectId(Long projectId, Pageable pageable) {
        return delegate.findByProjectId(projectId, pageable);
    }

    @Override
    public List<Questionnaire> findByProjectId(Long projectId) {
        return delegate.findByProjectId(projectId);
    }

    @Override
    public List<Questionnaire> findActiveByProjectAndDate(Long projectId, LocalDate today) {
        return delegate.findActiveByProjectAndDate(projectId, today);
    }

    @Override
    public List<Questionnaire> findQuestionnairesStartingToday(LocalDate today) {
        return delegate.findQuestionnairesStartingToday(today);
    }

    @Override
    public Optional<Questionnaire> findById(Integer questionnaireId) {
        return delegate.findById(questionnaireId);
    }

    @Override
    public Optional<Questionnaire> findByIdAndProjectId(Integer questionnaireId, Long projectId) {
        return delegate.findByIdAndProjectId(questionnaireId, projectId);
    }
}


package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres;

import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Question;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuestionRepositoryAdapter implements QuestionRepositoryPort {

    private final QuestionRepository delegate;

    @Override
    public Page<Question> findByQuestionnaireIdOrderByIdAsc(Integer questionnaireId, Pageable pageable) {
        return delegate.findByQuestionnaireIdOrderByIdAsc(questionnaireId, pageable);
    }

    @Override
    public Page<Question> searchByQuestionnaireId(Integer questionnaireId, String questionText, String roleName, Pageable pageable) {
        return delegate.searchByQuestionnaireId(questionnaireId, questionText, roleName, pageable);
    }

    @Override
    public Optional<Question> findById(Long id) {
        return delegate.findById(id);
    }
}


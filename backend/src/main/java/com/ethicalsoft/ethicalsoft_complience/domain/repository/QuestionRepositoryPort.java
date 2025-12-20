package com.ethicalsoft.ethicalsoft_complience.domain.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface QuestionRepositoryPort {
    Page<Question> findByQuestionnaireIdOrderByIdAsc(Integer questionnaireId, Pageable pageable);

    Page<Question> searchByQuestionnaireId(Integer questionnaireId, String questionText, String roleName, Pageable pageable);

    Optional<Question> findById(Long id);
}


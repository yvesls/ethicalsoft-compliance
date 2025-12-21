package com.ethicalsoft.ethicalsoft_complience.domain.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuestionnaireRepositoryPort {
    List<Questionnaire> findAllByProjectIdWithQuestions(Long projectId);

    Page<Questionnaire> findByProjectId(Long projectId, Pageable pageable);

    List<Questionnaire> findByProjectId(Long projectId);

    List<Questionnaire> findActiveByProjectAndDate(Long projectId, LocalDate today);

    List<Questionnaire> findQuestionnairesStartingToday(LocalDate today);

    Optional<Questionnaire> findById(Integer questionnaireId);

    Optional<Questionnaire> findByIdAndProjectId(Integer questionnaireId, Long projectId);
}


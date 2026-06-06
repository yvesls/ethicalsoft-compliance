package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.QuestionnaireResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireResultRepository extends JpaRepository<QuestionnaireResult, Long> {

    Optional<QuestionnaireResult> findByQuestionnaireId(Integer questionnaireId);

    List<QuestionnaireResult> findByProjectId(Long projectId);

    boolean existsByQuestionnaireId(Integer questionnaireId);

    @Query("SELECT qr FROM QuestionnaireResult qr WHERE qr.projectId = :projectId ORDER BY qr.calculatedAt ASC")
    List<QuestionnaireResult> findByProjectIdOrderedByDate(Long projectId);
}

